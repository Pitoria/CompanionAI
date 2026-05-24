package me.bray.companionai.utils;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;

public class CompanionNameUtil {

    public static String getBaseName(NPC npc) {
        String baseName = npc.data().get("companion-base-name");
        if (baseName == null) {
            // No le quites el color al nombre original por defecto
            baseName = npc.getName().split("\n")[0];
        }
        return baseName;
    }

    public static void updateHealthName(CompanionAI plugin, NPC npc) {
        if (npc == null || !npc.isSpawned()) return;

        String baseName = getBaseName(npc);
        npc.data().setPersistent("companion-base-name", baseName);

        // Si decidís apagar la vida en el nombre desde la config para usar hologramas
        if (!plugin.getConfig().getBoolean("settings.health-name.enabled", true)) {
            npc.setName(ChatColor.translateAlternateColorCodes('&', baseName));
            return;
        }

        double health = npc.getEntity() instanceof org.bukkit.entity.LivingEntity living
                ? living.getHealth()
                : 0;

        double maxHealth = npc.getEntity() instanceof Attributable attributable
                && attributable.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                ? attributable.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
                : 20;

        // Corregido: Ahora apunta a "health-format" como en tu config.yml
        String format = plugin.getConfig().getString(
                "settings.health-name.health-format",
                "&a%name% &c%health%/%max_health%"
        );

        String finalName = format
                .replace("%name%", baseName)
                .replace("%health%", String.valueOf((int) health))
                .replace("%max_health%", String.valueOf((int) maxHealth));

        npc.setName(ChatColor.translateAlternateColorCodes('&', finalName));
    }
}