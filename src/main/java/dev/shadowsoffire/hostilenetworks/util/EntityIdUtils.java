package dev.shadowsoffire.hostilenetworks.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for entity ID handling.
 * Maps datapack entity IDs to Minecraft 1.7.10 entity names.
 */
public final class EntityIdUtils {

    /** Maps datapack entity IDs to Minecraft 1.7.10 internal entity names */
    public static final Map<String, String> ENTITY_ID_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        // Entities with different internal names in Minecraft 1.7.10
        map.put("magma_cube", "LavaSlime");
        map.put("iron_golem", "VillagerGolem");
        map.put("snow_golem", "SnowMan");
        map.put("ender_dragon", "EnderDragon");
        map.put("wither", "WitherBoss");
        map.put("mooshroom", "MushroomCow");
        map.put("zombified_piglin", "PigZombie");
        map.put("wither_skeleton", "Skeleton");
        map.put("cave_spider", "CaveSpider");
        ENTITY_ID_MAP = Collections.unmodifiableMap(map);
    }

    private EntityIdUtils() {}

    /**
     * Get the Minecraft 1.7.10 internal entity name for a datapack entity ID.
     *
     * @param entityId the datapack entity ID (e.g., "magma_cube")
     * @return the internal entity name (e.g., "LavaSlime"), or the original ID if not mapped
     */
    public static String getInternalName(String entityId) {
        return ENTITY_ID_MAP.getOrDefault(entityId, entityId);
    }

    /**
     * Capitalize an entity ID for use with EntityList.
     * First checks the mapping, then falls back to simple capitalization.
     *
     * @param entityId the entity ID (e.g., "magma_cube" or "LavaSlime")
     * @return the capitalized name for EntityList lookup (e.g., "LavaSlime")
     */
    public static String getCapitalizedName(String entityId) {
        String mapped = ENTITY_ID_MAP.get(entityId);
        if (mapped != null) {
            return mapped;
        }
        // Check if it's already a capitalized form
        for (String key : ENTITY_ID_MAP.keySet()) {
            if (ENTITY_ID_MAP.get(key)
                .equals(entityId)) {
                return entityId; // Already capitalized
            }
        }
        // Default: first letter uppercase
        return entityId.substring(0, 1)
            .toUpperCase() + entityId.substring(1);
    }

    /**
     * Check if an entity ID needs special mapping.
     *
     * @param entityId the entity ID to check
     * @return true if this entity has a different internal name in 1.7.10
     */
    public static boolean needsMapping(String entityId) {
        return ENTITY_ID_MAP.containsKey(entityId);
    }
}
