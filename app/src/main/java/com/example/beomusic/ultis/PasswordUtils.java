package com.example.beomusic.ultis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for handling password hashing and verification
 */
public class PasswordUtils {

    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * Generates a random salt for password hashing
     * @return a random salt as byte array
     */
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Hashes a password with a given salt using SHA-256
     * @param password the password to hash
     * @param salt the salt to use
     * @return the hashed password
     */
    public static byte[] hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Creates a complete password hash with salt for storage
     * Format: base64(salt):base64(hash)
     * @param password the password to hash
     * @return the complete hash string for storage
     */
    public static String hashPasswordForStorage(String password) {
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(password, salt);
        
        String saltString = Base64.getEncoder().encodeToString(salt);
        String hashString = Base64.getEncoder().encodeToString(hash);
        
        return saltString + ":" + hashString;
    }
    
    /**
     * Verifies a password against a stored hash
     * @param password the password to verify
     * @param storedHash the stored hash from the database
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            // Split the stored hash into salt and hash components
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            
            // Hash the input password with the same salt
            byte[] actualHash = hashPassword(password, salt);
            
            // Compare the hashes
            if (actualHash.length != expectedHash.length) {
                return false;
            }
            
            // Time-constant comparison to prevent timing attacks
            int result = 0;
            for (int i = 0; i < actualHash.length; i++) {
                result |= actualHash[i] ^ expectedHash[i];
            }
            
            return result == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if a password meets minimum security requirements
     * @param password the password to check
     * @return true if the password is secure, false otherwise
     */
    public static boolean isPasswordSecure(String password) {
        // Minimum 8 characters
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }
        
        // Require at least 3 of the 4 character types
        int criteria = 0;
        if (hasLower) criteria++;
        if (hasUpper) criteria++;
        if (hasDigit) criteria++;
        if (hasSpecial) criteria++;
        
        return criteria >= 3;
    }
}
