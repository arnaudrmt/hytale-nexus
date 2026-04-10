package fr.arnaud.nexus.core;

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
 * Lightweight internationalization (i18n) manager.
 * <p>
 * Loads translations from {@code /lang/{locale}.properties} files.
 * Active locale is read from {@code /config.properties}; falls back to {@code en_US}.
 * If a key is missing in the primary locale, it falls back to {@code en_US}.
 */
public final class I18n {

    private static final String DEFAULT_LOCALE = "en_US";
    private static final String CONFIG_PATH = "/config.properties";
    private static final String LOCALE_KEY = "locale";

    private static final Map<String, String> translations = new HashMap<>();
    private static final Map<String, String> fallback = new HashMap<>();

    private static JavaPlugin plugin;

    private I18n() {
    }

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        String locale = resolveLocale();
        loadInto(DEFAULT_LOCALE, fallback);
        if (!locale.equals(DEFAULT_LOCALE)) {
            loadInto(locale, translations);
        }
    }

    /**
     * Returns the translated string for {@code key}, or the key itself if absent.
     */
    public static String t(String key) {
        return translations.getOrDefault(key, fallback.getOrDefault(key, key));
    }

    /**
     * Returns the translated string with {@code {0}}, {@code {1}} … placeholders replaced.
     */
    public static String t(String key, Object... args) {
        String value = t(key);
        for (int i = 0; i < args.length; i++) {
            value = value.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return value;
    }

    private static String resolveLocale() {
        try (InputStream stream = I18n.class.getResourceAsStream(CONFIG_PATH)) {
            if (stream == null) {
                plugin.getLogger().at(Level.WARNING).log("Missing config file: " + CONFIG_PATH + " — defaulting to " + DEFAULT_LOCALE);
                return DEFAULT_LOCALE;
            }
            Properties config = new Properties();
            config.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String locale = config.getProperty(LOCALE_KEY, DEFAULT_LOCALE).trim();
            return locale.isEmpty() ? DEFAULT_LOCALE : locale;
        } catch (IOException e) {
            plugin.getLogger().at(Level.SEVERE).log("Failed to read config file: " + CONFIG_PATH, e);
            return DEFAULT_LOCALE;
        }
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
