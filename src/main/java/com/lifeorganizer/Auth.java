package com.lifeorganizer;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Auth {
    private static final int ITERATIONS = 310_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Auth() {}

    static String hash(char[] password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
            return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing is unavailable.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    static boolean verify(char[] password, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 1_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            try {
                byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(actual, expected);
            } finally {
                spec.clearPassword();
            }
        } catch (IllegalArgumentException ex) {
            return false;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Password verification is unavailable.", ex);
        }
    }

    public static int register(String name, String email, char[] password) throws Exception {
        if (name.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || password.length < 8) {
            throw new IllegalArgumentException(
                "Enter a name, valid email, and password of at least 8 characters.");
        }
        try (Connection connection = Db.connect();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO users(name,email,password_hash) VALUES(?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name.trim());
            statement.setString(2, email.trim().toLowerCase(Locale.ROOT));
            statement.setString(3, hash(password));
            statement.executeUpdate();
            try (ResultSet results = statement.getGeneratedKeys()) {
                results.next();
                return results.getInt(1);
            }
        }
    }

    public static int login(String email, char[] password) throws Exception {
        try (Connection connection = Db.connect();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT user_id,password_hash FROM users WHERE email=?")) {
            statement.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next() || !verify(password, results.getString(2))) {
                    return -1;
                }
                return results.getInt(1);
            }
        }
    }
}
