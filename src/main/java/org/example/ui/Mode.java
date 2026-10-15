package org.example.ui;

public enum Mode {
    ENCRYPT("Encrypt", "Start encryption", "{name}.encrypted"),
    DECRYPT("Decrypt", "Start decryption", "{name}");

    final String label;
    final String startText;
    final String defaultFormat;

    Mode(String label, String startText, String defaultFormat) {
        this.label = label;
        this.startText = startText;
        this.defaultFormat = defaultFormat;
    }

    @Override
    public String toString() {
        return label;
    }
}
