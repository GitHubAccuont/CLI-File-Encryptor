package org.example.service;

import org.example.utils.CryptoUtils;
import org.example.utils.Header;
import org.example.utils.PathwayUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;


public class EncryptionService {

    public Map<File, Path> encryptAll(Set<File> targets, String password,
                                      String format, Path dest) throws IOException {
        Set<String> used = new HashSet<>();
        Map<File, Path> result = new LinkedHashMap<>();
        for (File f : targets) {
            String base = PathwayUtils.buildName(f, format);
            Path out = PathwayUtils.resolveUnique(dest, base, used);
            try (var rawIn = Files.newInputStream(f.toPath());
                 var in = new BufferedInputStream(rawIn);
                 var os = Files.newOutputStream(out)) {
                CryptoUtils.encryptFile(password, PathwayUtils.extensionOf(f), in, os);
            } catch (GeneralSecurityException e) {
                throw new IOException("Encryption failed for " + f.getName() + ": " + e.getMessage(), e);
            }
            result.put(f, out);
        }
        return result;
    }

    public Map<File, Path> decryptAll(Set<File> targets, String password,
                                      String format, Path dest) throws IOException {
        Set<File> encrypted = targets.stream()
                .filter(e -> e.getName().toLowerCase().endsWith(".encrypted"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> used = new HashSet<>();
        Map<File, Path> result = new LinkedHashMap<>();

        for (File f : encrypted) {
            String base = PathwayUtils.buildName(f, format);
            try (var rawIn = Files.newInputStream(f.toPath());
                 var in = new DataInputStream(new BufferedInputStream(rawIn))) {

                Header h = CryptoUtils.readHeader(in);

                String name = h.getExtension().isEmpty() ? base : base + "." + h.getExtension();

                Path out = PathwayUtils.resolveUnique(dest, name, used);
                Path tmp = Files.createTempFile(dest, ".decrypt-", ".tmp");
                boolean moved = false;
                try {
                    try (var os = Files.newOutputStream(tmp)) {
                        CryptoUtils.decryptFile(h, password, in, os);
                    }
                    Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
                    moved = true;
                } finally {
                    if (!moved) Files.deleteIfExists(tmp);
                }
                result.put(f, out);
            } catch (GeneralSecurityException e) {
                throw new IOException("Decryption failed for " + f.getName() + ": " + e.getMessage(), e);
            }
        }
        return result;
    }
}
