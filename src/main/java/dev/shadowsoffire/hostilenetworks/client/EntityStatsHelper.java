package dev.shadowsoffire.hostilenetworks.client;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import dev.shadowsoffire.hostilenetworks.util.EntityIdUtils;

/**
 * Utility class for getting entity statistics for GUI display.
 * Uses real entity instances to get health, armor, and XP reward.
 * Client-side only.
 */
public class EntityStatsHelper {

    /**
     * Get all stats for an entity as an array: [health, armor, xp]
     *
     * @param entityId The entity ID string (e.g., "zombie", "minecraft:skeleton")
     * @return Array of stat strings, or obfuscated text for non-LivingEntity
     */
    public static String[] getAllStats(String entityId) {
        return new String[] { getHealthStat(entityId), getArmorStat(entityId), getXpStat(entityId) };
    }

    /**
     * Get the max health of an entity (divided by 2).
     */
    public static String getHealthStat(String entityId) {
        Entity entity = createEntity(entityId);
        if (entity instanceof EntityLivingBase) {
            float maxHealth = ((EntityLivingBase) entity).getMaxHealth();
            return String.valueOf((int) (maxHealth / 2));
        }
        return getObfuscatedText();
    }

    /**
     * Get the armor value of an entity (divided by 2).
     */
    public static String getArmorStat(String entityId) {
        Entity entity = createEntity(entityId);
        if (entity instanceof EntityLivingBase) {
            int armor = ((EntityLivingBase) entity).getTotalArmorValue();
            return String.valueOf(armor / 2);
        }
        return getObfuscatedText();
    }

    /**
     * Get the base XP reward of an entity.
     */
    public static String getXpStat(String entityId) {
        Entity entity = createEntity(entityId);
        if (entity instanceof EntityLiving) {
            int xp = getExperienceValue((EntityLiving) entity);
            return String.valueOf(xp);
        }
        return getObfuscatedText();
    }

    /**
     * Create an entity instance from its ID.
     */
    private static Entity createEntity(String entityId) {
        World world = getRenderWorld();
        if (world == null) {
            return null;
        }

        Entity entity = null;
        String[] namesToTry = createEntityNameVariants(entityId);

        for (String name : namesToTry) {
            entity = net.minecraft.entity.EntityList.createEntityByName(name, world);
            if (entity != null) {
                break;
            }
        }

        return entity;
    }

    /**
     * Get the render world from Minecraft.
     */
    private static World getRenderWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null ? mc.theWorld : null;
    }

    /**
     * Create variations of entity names to try.
     */
    private static String[] createEntityNameVariants(String entityId) {
        if (!entityId.contains(":")) {
            String withPrefix = "minecraft:" + entityId;
            if (net.minecraft.entity.EntityList.createEntityByName(withPrefix, null) != null) {
                return new String[] { withPrefix };
            }
        }

        if (net.minecraft.entity.EntityList.createEntityByName(entityId, null) != null) {
            return new String[] { entityId };
        }

        String capitalized = entityId.substring(0, 1)
            .toUpperCase() + entityId.substring(1);
        if (net.minecraft.entity.EntityList.createEntityByName(capitalized, null) != null) {
            return new String[] { capitalized };
        }

        String mappedName = EntityIdUtils.getInternalName(entityId);
        if (!mappedName.equals(capitalized)
            && net.minecraft.entity.EntityList.createEntityByName(mappedName, null) != null) {
            return new String[] { mappedName };
        }

        return new String[] { "minecraft:" + entityId, entityId, capitalized, mappedName };
    }

    /**
     * Get the experience value from an EntityLiving using reflection.
     * The experienceValue field is protected.
     */
    private static int getExperienceValue(EntityLiving entity) {
        try {
            Field field = EntityLiving.class.getDeclaredField("experienceValue");
            field.setAccessible(true);
            return field.getInt(entity);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get obfuscated text for non-LivingEntity stats.
     */
    private static String getObfuscatedText() {
        return "\u00A7k99999";
    }
}
