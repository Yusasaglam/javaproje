package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

    private HashUtil() {}

    public static String sha256(String metin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(metin.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 desteklenmiyor", e);
        }
    }

    /** 64 karakterli hex string mi? (SHA-256 çıktısı) */
    public static boolean hashMi(String s) {
        return s != null && s.length() == 64 && s.matches("[0-9a-f]+");
    }
}
