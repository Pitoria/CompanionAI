package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import me.bray.companionai.utils.CompanionCombatManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CompanionAssistListener implements Listener {

    private final CompanionAI plugin;

    public CompanionAssistListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onOwnerAttack(EntityDamageByEntityEvent event) {
        Player player = null;

        if (event.getDamager() instanceof Player p) {
            player = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            player = p;
        }

        if (player == null) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof Player) {
            return;
        }

        NPC npc = getPlayerCompanion(player);

        if (npc == null || !npc.isSpawned()) {
            return;
        }

        if (target.equals(npc.getEntity())) {
            return;
        }

        // El mánager ahora toma el control absoluto según el ítem equipado
        new CompanionCombatManager(plugin).executeAttack(npc, target, player);
    }

    private NPC getPlayerCompanion(Player player) {
        String path = "players." + player.getUniqueId() + ".npc-id";
        if (!plugin.getDataManager().getData().contains(path)) {
            return null;
        }
        int npcId = plugin.getDataManager().getData().getInt(path);
        return CitizensAPI.getNPCRegistry().getById(npcId);
    }
}