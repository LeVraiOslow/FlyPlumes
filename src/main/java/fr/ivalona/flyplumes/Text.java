package fr.ivalona.flyplumes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Convertit du texte configure (codes &amp;, hexadecimal, balises MiniMessage) en Component.
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    /** Texte colore classique (messages de chat). */
    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MM.deserialize(legacyToMini(input));
    }

    /** Texte pour un item : pas d'italique par defaut. */
    public static Component item(String input) {
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(parse(input));
    }

    public static List<Component> itemLines(List<String> lines) {
        List<Component> out = new ArrayList<>();
        for (String line : lines) {
            out.add(item(line));
        }
        return out;
    }

    private static String legacyToMini(String in) {
        StringBuilder out = new StringBuilder(in.length() + 16);
        int len = in.length();
        for (int i = 0; i < len; i++) {
            char c = in.charAt(i);
            if (c != '&' || i + 1 >= len) {
                out.append(c);
                continue;
            }
            char next = in.charAt(i + 1);

            // &#RRGGBB
            if (next == '#' && isHex(in, i + 2, 6)) {
                out.append("<reset><#").append(in, i + 2, i + 8).append('>');
                i += 7;
                continue;
            }

            String tag = tagFor(Character.toLowerCase(next));
            if (tag == null) {
                out.append(c);
                continue;
            }
            // Comme en legacy : une couleur remet a zero les styles precedents.
            if (isColor(Character.toLowerCase(next))) {
                out.append("<reset>");
            }
            out.append('<').append(tag).append('>');
            i++;
        }
        return out.toString();
    }

    private static boolean isHex(String s, int from, int count) {
        if (from + count > s.length()) {
            return false;
        }
        for (int i = from; i < from + count; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isColor(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private static String tagFor(char c) {
        return switch (c) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
    }
}
