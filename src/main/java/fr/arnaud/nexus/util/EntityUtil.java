package fr.arnaud.nexus.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityUtil {

    public static boolean isSameEntityRef(Ref<EntityStore> a, Ref<EntityStore> b) {
        return a.getIndex() == b.getIndex();
    }
}
