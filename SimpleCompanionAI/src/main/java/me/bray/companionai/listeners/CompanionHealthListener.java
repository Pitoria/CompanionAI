package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import me.bray.companionai.utils.CompanionNameUtil;

public class CompanionHealthListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());

        if (npc == null) {
            return;
        }

        CompanionNameUtil.updateHealthName(plugin, npc);
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent event) {

        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());

        if (npc == null) {
            return;
        }

        CompanionNameUtil.updateHealthName(plugin, npc);
    }

    private final CompanionAI plugin;

    public CompanionHealthListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

}