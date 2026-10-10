package org.example;

import org.example.utils.CryptoUtils;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: <encrypt|decrypt> <input> <output> <password>");
            return;
        }

        String mode = args[0];
        File input = new File(args[1]);
        File output = new File(args[2]);
        String password = args[3];

        CryptoUtils crypto = new CryptoUtils();

        try {
            if (mode.equals("encrypt")) {
                crypto.encryptFile(password, input, output);
                System.out.println("Encrypted: " + output.getPath());
            } else if (mode.equals("decrypt")) {
                crypto.decryptFile(password, input, output);
                System.out.println("Decrypted: " + output.getPath());
            } else {
                System.out.println("Unknown mode: " + mode + " (use 'encrypt' or 'decrypt')");
            }
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}