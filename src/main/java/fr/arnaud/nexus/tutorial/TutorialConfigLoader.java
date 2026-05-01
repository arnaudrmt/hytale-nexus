package fr.arnaud.nexus.tutorial;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.arnaud.nexus.core.Nexus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public final class TutorialConfigLoader {

    private static final String PATH = "/nexus/tutorial/tutorial.json";
    private static final Gson GSON = new Gson();

    private TutorialConfigLoader() {
    }

    public static List<TutorialStepConfig> load() {
        InputStream stream = Nexus.class.getResourceAsStream(PATH);
        if (stream == null) {
            Nexus.getInstance().getLogger().at(Level.SEVERE).log("Tutorial config not found: " + PATH);
            return List.of();
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            List<TutorialStepConfig> steps = new ArrayList<>();

            for (JsonElement el : root.getAsJsonArray("steps")) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String titleKey = obj.get("titleKey").getAsString();
                String bodyKey = obj.get("bodyKey").getAsString();
                String triggerRaw = obj.get("triggerType").getAsString().toUpperCase();
                TutorialTriggerType trigger = TutorialTriggerType.valueOf(triggerRaw);
                float displaySeconds = obj.has("displaySeconds")
                    ? obj.get("displaySeconds").getAsFloat() : 0f;

                steps.add(new TutorialStepConfig(id, titleKey, bodyKey, trigger, displaySeconds));
            }

            return Collections.unmodifiableList(steps);
        } catch (Exception e) {
            Nexus.getInstance().getLogger().at(Level.SEVERE)
                 .log("Failed to parse tutorial config: " + e.getMessage());
            return List.of();
        }
    }
}
