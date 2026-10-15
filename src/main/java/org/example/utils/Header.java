package org.example.utils;

public class Header {

    public int getVersion() {
        return version;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getIv() {
        return iv;
    }

    public String getExtension() {
        return extension;
    }

    private final int version;
    private final byte[] salt;
    private final byte[] iv;
    private final String extension;


    public Header(int version, byte[] salt, byte[] iv, String extension) {
        this.version = version;
        this.salt = salt;
        this.iv = iv;
        this.extension = extension;
    }
}
