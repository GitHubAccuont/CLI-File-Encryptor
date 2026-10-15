package org.example.ui;

import org.example.service.EncryptionService;
import org.example.utils.PathwayUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SimpleUi {

    private static final int PANEL_WIDTH = 640;
    private static final int PANEL_HEIGHT = 340;
    private static final int CONTROL_WIDTH = PANEL_WIDTH / 3;

    private final EncryptionService encryptionService;
    private final Border defaultFormatBorder;
    private final Border invalidBorder;

    private JPanel mainPanel;
    private JPanel controlPanel;
    private JButton selectButton;
    private JButton startButton;
    private JButton outputSelector;
    private JPasswordField passwordField;
    private JTextField formatField;
    private JList<File> fileList;
    private DefaultListModel<File> fileModel;
    private JFileChooser inputChooser;
    private JFileChooser outputChooser;
    private JLabel outputLabel;
    private Path outputPath;
    private JComboBox<Mode> modeSelector;

    public SimpleUi() {
        this.encryptionService = new EncryptionService();
        this.defaultFormatBorder = new JTextField().getBorder();
        this.invalidBorder = BorderFactory.createLineBorder(new Color(200, 50, 50), 1);
        initUi();
    }

    // UI setup here

    private void initUi() {
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        createSelectionList();
        createControlPanel();
        initSelectors();
        linkButtons();
        applyMode(Mode.ENCRYPT);

        mainPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);
    }

    private void createSelectionList() {
        fileModel = new DefaultListModel<>();
        fileList = new JList<>(fileModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                File f = (File) value;
                setText(f.getAbsolutePath());
                setToolTipText(f.getAbsolutePath());
                return this;
            }
        });
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setPreferredSize(new Dimension(CONTROL_WIDTH, PANEL_HEIGHT));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;

        c.gridy = 0;
        controlPanel.add(new JLabel("Password:"), c);
        c.gridy = 1;
        passwordField = new JPasswordField();
        passwordField.setColumns(12);
        controlPanel.add(passwordField, c);

        c.gridy = 2;
        controlPanel.add(Box.createVerticalStrut(4), c);
        c.gridy = 3;
        controlPanel.add(new JLabel("Mode:"), c);
        c.gridy = 4;
        modeSelector = new JComboBox<>(Mode.values());
        controlPanel.add(modeSelector, c);

        c.gridy = 5;
        controlPanel.add(new JLabel("Name format:"), c);
        c.gridy = 6;
        formatField = new JTextField();
        formatField.setToolTipText("Placeholders: {name}, {date}, {ext}. e.g. {name}.encrypted");
        controlPanel.add(formatField, c);
        formatField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateFormatField();
            }
        });

        c.gridy = 7;
        controlPanel.add(Box.createVerticalStrut(6), c);
        c.gridy = 8;
        outputLabel = new JLabel("Output: not set");
        controlPanel.add(outputLabel, c);
        c.gridy = 9;
        outputSelector = new JButton("Choose output folder…");
        controlPanel.add(outputSelector, c);

        c.gridy = 10;
        c.weighty = 1.0;
        controlPanel.add(Box.createVerticalStrut(6), c);
        c.weighty = 0.0;
        c.gridy = 11;
        selectButton = new JButton("Select files");
        controlPanel.add(selectButton, c);
        c.gridy = 12;
        startButton = new JButton("Start");
        controlPanel.add(startButton, c);
    }

    private void initSelectors() {
        inputChooser = new JFileChooser();
        inputChooser.setMultiSelectionEnabled(true);

        outputChooser = new JFileChooser();
        outputChooser.setDialogTitle("Select output folder");
        outputChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        outputChooser.setMultiSelectionEnabled(false);
        outputChooser.setAcceptAllFileFilterUsed(false);
    }

    private void linkButtons() {
        selectButton.addActionListener(e -> onSelectFiles());
        startButton.addActionListener(e -> onStart());
        outputSelector.addActionListener(e -> onOutputSelect());
        modeSelector.addActionListener(e ->
                applyMode((Mode) modeSelector.getSelectedItem()));
    }

    private void applyMode(Mode mode) {
        if (mode == null) return;

        startButton.setText(mode.startText);
        formatField.setText(mode.defaultFormat);
        formatField.setEnabled(true);
        validateFormatField();

        inputChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                boolean isEnc = f.getName().toLowerCase().endsWith(".encrypted");
                return (mode == Mode.ENCRYPT) != isEnc;
            }

            @Override
            public String getDescription() {
                return mode == Mode.ENCRYPT
                        ? "Files to encrypt (anything but *.encrypted)"
                        : "Encrypted files (*.encrypted)";
            }
        });
        inputChooser.setDialogTitle(mode == Mode.ENCRYPT
                ? "Select files to encrypt" : "Select files to decrypt");
    }

    private void validateFormatField() {
        String text = formatField.getText();
        boolean ok;
        try {
            ok = text != null && !text.isBlank() && PathwayUtils.validate(text);
        } catch (RuntimeException ex) {
            ok = false;
        }
        formatField.setBorder(ok ? defaultFormatBorder : invalidBorder);
        formatField.setToolTipText(ok
                ? "Placeholders: {name}, {date}, {ext}."
                : "Invalid name format.");
    }

    private void onOutputSelect() {
        int result = outputChooser.showOpenDialog(mainPanel);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File folder = outputChooser.getSelectedFile();
        if (folder == null || !folder.isDirectory()) return;

        outputPath = folder.toPath();
        outputLabel.setText("<html>Output:<br><i>" + outputPath + "</i></html>");
    }

    private void onSelectFiles() {
        int result = inputChooser.showOpenDialog(mainPanel);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File[] files = inputChooser.getSelectedFiles();
        if (files == null || files.length == 0) return;

        FileFilter filter = inputChooser.getFileFilter();
        fileModel.clear();
        for (File f : files) {
            if (f.isDirectory()) {
                expandDirectory(f, filter);
            } else if (filter.accept(f)) {
                fileModel.addElement(f);
            }
        }
    }

    private void expandDirectory(File dir, FileFilter filter) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isFile() && filter.accept(child)) {
                fileModel.addElement(child);
            }
        }
    }

    private void onStart() {
        if (fileModel.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel, "No files selected.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (outputPath == null) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Please choose an output folder first.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Files.isWritable(outputPath)) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Output folder is not writable:\n" + outputPath,
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        char[] raw = passwordField.getPassword();
        String password = new String(raw);
        Arrays.fill(raw, '\0');

        if (password.isEmpty() || !PathwayUtils.validatePassword(password)) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Password must be at least 8 characters and contain no whitespace.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Mode mode = (Mode) modeSelector.getSelectedItem();
        String format = formatField.getText();
        if (!PathwayUtils.validate(format)) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Name format is invalid. Fix the highlighted field.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Set<File> files = new LinkedHashSet<>();
        for (int i = 0; i < fileModel.size(); i++) files.add(fileModel.get(i));

        try {
            Map<File, Path> results;
            if (mode == Mode.ENCRYPT) {
                results = encryptionService.encryptAll(files, password, format, outputPath);
            } else {
                results = encryptionService.decryptAll(files, password, format, outputPath);
            }
            if (results.size() < files.size()) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Processed " + results.size() + " of " + files.size()
                                + " file(s). The rest were skipped (not .encrypted).",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainPanel,
                        "Done. " + results.size() + " file(s) processed.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel,
                    (mode == Mode.ENCRYPT ? "Encryption" : "Decryption")
                            + " failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getContextPanel() {
        return mainPanel;
    }
}