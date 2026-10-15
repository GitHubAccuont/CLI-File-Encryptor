package org.example.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathwayUtils {

    private static final Pattern LITERAL = Pattern.compile("[A-Za-z0-9 _\\-.]+");
    private static final Pattern PASS_LITERAL = Pattern.compile("\\S+");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(name|date|ext)\\}");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static boolean validate(String format) {
        if (format == null || format.isBlank() || LITERAL.matcher(format).matches()) return false;

        int open = -1;
        boolean sawPlaceholder = false;

        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);

            if (c == '{') {
                if (open >= 0) return false;
                open = i;
            } else if (c == '}') {
                if (open < 0) return false;
                String flag = format.substring(open + 1, i);
                if (!isKnownFlag(flag)) return false;
                open = -1;
                sawPlaceholder = true;
            }
        }
        return open < 0 && sawPlaceholder;
    }

    public static String buildName(File source, String format) {

        String now = LocalDateTime.now().format(FORMATTER);

        Matcher m = PLACEHOLDER.matcher(format);
        StringBuilder out = new StringBuilder();

        while (m.find()) {
            String value;
            switch (m.group(1)) {
                case "name":
                    value = stemOf(source);
                    break;
                case "date":
                    value = now;
                    break;
                case "ext":
                    value = extensionOf(source);
                    break;
                default:
                    throw new IllegalStateException("unreachable: " + m.group(1));
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String stemOf(File source) {
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    public static String extensionOf(File source) {
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(dot + 1) : "";
    }

    private static boolean isKnownFlag(String s) {
        switch (s) {
            case "name":
            case "date":
            case "ext":
                return true;
            default:
                return false;
        }
    }

    public static boolean validatePassword(String password) {
        return password != null && !password.isEmpty()
                && PASS_LITERAL.matcher(password).matches()
                && password.length() >= 8;
    }

    public static Path resolveUnique(Path dest, String base, Set<String> used) {
        String candidate = base;
        int n = 1;
        while (used.contains(candidate) || Files.exists(dest.resolve(candidate))) {
            candidate = insertSuffix(base, n++);
        }
        used.add(candidate);
        return dest.resolve(candidate);
    }

    private static String insertSuffix(String name, int n) {
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) + "_" + n + name.substring(dot) : name + "_" + n;
    }
}
