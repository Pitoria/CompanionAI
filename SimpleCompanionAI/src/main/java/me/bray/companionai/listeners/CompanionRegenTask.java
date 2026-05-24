package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CompanionRegenTask {

    private final CompanionAI plugin;

    public CompanionRegenTask(CompanionAI plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    private void tick() {
        // Cambiado a data.yml
        if (!plugin.getDataManager().getData().contains("players")) return;

        for (String uuid : plugin.getDataManager().getData().getConfigurationSection("players").getKeys(false)) {
            int npcId = plugin.getDataManager().getData().getInt("players." + uuid + ".npc-id");
            NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

            if (npc == null || !npc.isSpawned()) continue;

            // LivingEntity para soportar cualquier tipo de entidad de NPC de manera segura
            if (!(npc.getEntity() instanceof LivingEntity npcEntity)) continue;

            // Obtener la vida máxima mediante atributos
            double max = npcEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                    ? npcEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
                    : 20.0;

            double current = npcEntity.getHealth();

            if (current >= max) continue;

            npcEntity.setHealth(Math.min(max, current + 2.0));
        }
    }
}