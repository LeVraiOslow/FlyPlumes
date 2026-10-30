package fr.ivalona.flyplumes;

import org.bukkit.configuration.ConfigurationSection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Affichage et lecture des durees. */
public final class TimeFormat {

    private static final Pattern DURATION =
            Pattern.compile("^(?:(\\d+)h)?(?:(\\d+)m(?:in)?)?(?:(\\d+)s)?$", Pattern.CASE_INSENSITIVE);

    private final FlyPlugin plugin;

    public TimeFormat(FlyPlugin plugin) {
        this.plugin = plugin;
    }

    private String opt(String key, String def) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("time-format");
        return s == null ? def : s.getString(key, def);
    }

    public String infinite() {
        return opt("infinite", "infini");
    }

    /** 3725 -> "1h 2min 5s" (les unites a zero sont omises). */
    public String format(long totalSeconds) {
        if (totalSeconds <= 0) {
            return opt("zero", "0s");
        }
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        String sep = opt("separator", " ");
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append(opt("hours", "h"));
        }
        if (m > 0) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(m).append(opt("minutes", "min"));
        }
        if (s > 0) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(s).append(opt("seconds", "s"));
        }
        return sb.toString();
    }

    /** "90", "45s", "10m", "1h30m" -> secondes. Retourne -1 si invalide. */
    public static long parse(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }
        if (input.chars().allMatch(Character::isDigit)) {
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        Matcher m = DURATION.matcher(input);
        if (!m.matches()) {
            return -1;
        }
        long total = 0;
        if (m.group(1) != null) {
            total += Long.parseLong(m.group(1)) * 3600;
        }
        if (m.group(2) != null) {
            total += Long.parseLong(m.group(2)) * 60;
        }
        if (m.group(3) != null) {
            total += Long.parseLong(m.group(3));
        }
        return total;
    }
}
