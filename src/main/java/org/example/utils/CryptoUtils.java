package org.example.utils;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;

public class CryptoUtils {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int SALT_LENGTH = 16;
    private static final int VERSION = 1;

    public void encryptFile(String password, File inputFile, File outputFile) {

        try {

            SecureRandom random = new SecureRandom();
            byte[] salt = random.generateSeed(SALT_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKey tmp = factory.generateSecret(keySpec);

            SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();

            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(outputFile)))) {

                out.writeInt(VERSION);
                out.writeInt(salt.length);
                out.write(salt);
                out.writeInt(iv.length);
                out.write(iv);

                try (InputStream in = new FileInputStream(inputFile);
                     CipherInputStream encrypted = new CipherInputStream(in, cipher)) {
                    encrypted.transferTo(out);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not read file by the name:" + inputFile.getName());
        }
    }


    public void decryptFile(String password, File inputFile, File outputFile) {

        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(inputFile)))) {

            int version = input.readInt();

            if(version != VERSION) {
                throw new RuntimeException("Version mismatch");
            }
            byte[] salt = input.readNBytes(input.readInt());
            byte[] iv = input.readNBytes(input.readInt());

            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);

            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKey tmp = factory.generateSecret(keySpec);

            SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            try (CipherInputStream decrypted = new CipherInputStream(new BufferedInputStream(input), cipher)) {
                try(OutputStream out = new FileOutputStream(outputFile)){
                    decrypted.transferTo(out);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}