package fr.arnaud.nexus.i18n;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Lightweight i18n system.
 *
 * Loads key=value pairs from /lang/{locale}.properties bundled in the jar.
 * Falls back to en_US if a key is missing in the active locale.
 *
 * Usage: I18n.t("flow.segment.lost")
 */
public final class I18n {

    private static final String DEFAULT_LOCALE = "en_US";
    private static final Map<String, String> translations = new HashMap<>();
    private static final Map<String, String> fallback = new HashMap<>();

    private static JavaPlugin plugin;

    private I18n() {}

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        String locale = resolveLocale();
        loadInto(DEFAULT_LOCALE, fallback);
        if (!locale.equals(DEFAULT_LOCALE)) {
            loadInto(locale, translations);
        }
    }

    /**
     * Returns the translated string for {@code key}.
     * Falls back to the en_US value, then to the raw key if not found.
     */
    public static String t(String key) {
        return translations.getOrDefault(key, fallback.getOrDefault(key, key));
    }

    /**
     * Returns the translated string with simple positional replacements.
     * Example: I18n.t("flow.gained", 2) where string is "Flow gained: {0}"
     */
    public static String t(String key, Object... args) {
        String value = t(key);
        for (int i = 0; i < args.length; i++) {
            value = value.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String resolveLocale() {
        // TODO: Read from plugin config file once config API is stable.
        return DEFAULT_LOCALE;
    }

    private static void loadInto(String locale, Map<String, String> target) {
        String path = "/lang/" + locale + ".properties";
        try (InputStream stream = I18n.class.getResourceAsStream(path)) {
            if (stream == null) {
                plugin.getLogger().at(Level.WARNING).log("Missing lang file: " + path);
                return;
            }
            Properties props = new Properties();
            props.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String key : props.stringPropertyNames()) {
                target.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            plugin.getLogger().at(Level.SEVERE).log("Failed to load lang file: " + path, e);
        }
    }
}
