package org.example.utils;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoUtils {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int SALT_LENGTH = 16;
    private static final int VERSION = 1;
    private static final int PBKDF2_ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 128;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    /**
     * Encrypts {@code in} to {@code out}.
     * Neither stream is closed; the caller owns them. {@code out} is flushed.
     */
    public static void encryptFile(String password, String extension, InputStream in, OutputStream out)
            throws IOException, GeneralSecurityException {

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();

        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(out));

        os.writeInt(VERSION);

        os.writeInt(salt.length);
        os.write(salt);

        os.writeInt(iv.length);
        os.write(iv);

        byte[] extBytes = extension == null ? new byte[0] : extension.getBytes(StandardCharsets.UTF_8);
        os.writeInt(extBytes.length);
        os.write(extBytes);

        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            byte[] chunk = cipher.update(buf, 0, n);
            if (chunk != null) os.write(chunk);
        }
        byte[] tail = cipher.doFinal();
        if (tail != null) os.write(tail);
        os.flush();
    }

    /**
     * Decrypts {@code inputFile} to {@code outputFile}.
     * Neither stream is closed; the caller owns them. {@code outputFile} is flushed.
     *
     * @throws GeneralSecurityException if the password is wrong or the file is corrupted
     *                                  (specifically, an {@link AEADBadTagException} is
     *                                  wrapped as the cause)
     */
    public static void decryptFile(Header h, String password, InputStream input, OutputStream output)
            throws IOException, GeneralSecurityException {

        SecretKey key = deriveKey(password, h.getSalt());
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, h.getIv()));

        BufferedOutputStream out = new BufferedOutputStream(output);
        byte[] buffer = new byte[8192];
        int n;
        try {
            while ((n = input.read(buffer)) != -1) {
                byte[] chunk = cipher.update(buffer, 0, n);
                if (chunk != null) out.write(chunk);
            }
            byte[] tail = cipher.doFinal();
            if (tail != null) out.write(tail);
            out.flush();
        } catch (AEADBadTagException e) {
            throw new GeneralSecurityException("Wrong password or corrupted file", e);
        }
    }

    private static SecretKey deriveKey(String password, byte[] salt)
            throws GeneralSecurityException {
        char[] passwordChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(
                passwordChars, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

        try {
            byte[] encodedKey = SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(spec)
                    .getEncoded();
            return new SecretKeySpec(encodedKey, "AES");
        } finally {
            spec.clearPassword();
            Arrays.fill(passwordChars, '\0');
        }
    }

    /**
     * Method used to retrieve header holding data required to decrypt files (e.g. extension, salt and IV)
     *
     * @param input {@link java.io.DataInputStream}
     * @return {@link Header}
     * @throws IOException
     */
    public static Header readHeader(DataInputStream input) throws IOException {
        int version = input.readInt();
        if (version != VERSION) throw new IOException("Unsupported version: " + version);

        int saltLen = input.readInt();
        if (saltLen <= 0 || saltLen > 64) throw new IOException("Bad salt length: " + saltLen);
        byte[] salt = input.readNBytes(saltLen);
        if (salt.length != saltLen) throw new IOException("Truncated salt");

        int ivLen = input.readInt();
        if (ivLen != GCM_IV_BYTES) throw new IOException("Bad IV length: " + ivLen);
        byte[] iv = input.readNBytes(ivLen);
        if (iv.length != ivLen) throw new IOException("Truncated IV");

        int extLen = input.readInt();
        if (extLen < 0 || extLen > 64) throw new IOException("Bad extension length: " + extLen);
        byte[] extBytes = input.readNBytes(extLen);
        if (extBytes.length != extLen) throw new IOException("Truncated extension");

        return new Header(version, salt, iv, new String(extBytes, StandardCharsets.UTF_8));
    }
}