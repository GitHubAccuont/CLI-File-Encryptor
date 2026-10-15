package org.example;

import org.example.ui.SimpleUi;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame window = new JFrame("File decryptor");

            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            window.setContentPane(new SimpleUi().getContextPanel());

            window.setLocationRelativeTo(null);

            window.pack();

            window.setVisible(true);
        });
    }
}