package me.bray.companionai.utils;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtil {

    public static String msg(CompanionAI plugin, String path, NPC npc, Player player) {

        String message = plugin.getMessages().getString(path);

        if (message == null) {
            plugin.getLogger().warning("Missing message: " + path);
            return ChatColor.RED + "Missing message: " + path;
        }

        message = ChatColor.translateAlternateColorCodes('&', message);

        if (player != null) {
            message = message.replace("%player%", player.getName());
        }

        if (npc != null) {
            // CORREGIDO: Extraemos el nombre base limpio/coloreado usando tu Util
            String baseName = CompanionNameUtil.getBaseName(npc);
            message = message.replace("%companion%", baseName);
            message = message.replace("%id%", String.valueOf(npc.getId()));
            message = message.replace("%spawned%", String.valueOf(npc.isSpawned()));

            // CORREGIDO SEGURO: Cálculo de vida sin romper por culpa de casteos a Player fijos
            if (npc.isSpawned() && npc.getEntity() instanceof org.bukkit.entity.LivingEntity livingEntity) {
                message = message.replace("%health%", String.valueOf((int) livingEntity.getHealth()));

                double maxHealth = livingEntity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null
                        ? livingEntity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()
                        : 20.0;
                message = message.replace("%max_health%", String.valueOf((int) maxHealth));
            } else {
                // Valores por defecto en caso de que el NPC esté desbaneado/lejos
                message = message.replace("%health%", "0");
                message = message.replace("%max_health%", "20");
            }

            // CORREGIDO: Reemplazo del %state% buscando en tu data.yml mediante DataManager
            if (player != null) {
                String statePath = "players." + player.getUniqueId() + ".state";
                String state = plugin.getDataManager().getData().getString(statePath, "STAY");
                message = message.replace("%state%", state);
            }

            // Reemplazo de la skin desde data.yml si existe
            if (player != null) {
                String skin = plugin.getDataManager().getData().getString(
                        "players." + player.getUniqueId() + ".skin"
                );
                if (skin != null) {
                    message = message.replace("%skin%", skin);
                } else {
                    message = message.replace("%skin%", "Default");
                }
            }
        }

        // Volvemos a pasar un filtro final por si las variables agregadas (%companion%, etc.) traían códigos de color propios.
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}