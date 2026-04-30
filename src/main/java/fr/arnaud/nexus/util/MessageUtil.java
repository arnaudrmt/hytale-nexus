package fr.arnaud.nexus.util;

import com.hypixel.hytale.server.core.Message;

public class MessageUtil {

    public static String componentNotRegistered(String componentName) {
        return Message.translation("nexus.component.not.registered").param("component", componentName).getRawText();
    }

    public static String unknown() {
        return Message.translation("nexus.unknown").getRawText();
    }
}
