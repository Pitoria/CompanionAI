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

public class CompanionProtectListener implements Listener {

    private final CompanionAI plugin;

    public CompanionProtectListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    // 1. SI TE ATACAN A VOS
    @EventHandler
    public void onOwnerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        LivingEntity attacker = getRealAttacker(event);
        if (attacker == null || attacker instanceof Player) {
            return;
        }

        String path = "players." + player.getUniqueId() + ".npc-id";
        if (!plugin.getDataManager().getData().contains(path)) {
            return;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

        if (npc == null || !npc.isSpawned()) {
            return;
        }

        if (attacker.equals(npc.getEntity())) {
            return;
        }

        // Derivamos al mánager de combate
        new CompanionCombatManager(plugin).executeAttack(npc, attacker, player);
    }

    // 2. SI LO ATACAN A ÉL
    @EventHandler
    public void onCompanionDamage(EntityDamageByEntityEvent event) {
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null || !npc.isSpawned()) {
            return;
        }

        if (!isCompanion(npc)) {
            return;
        }

        LivingEntity attacker = getRealAttacker(event);
        if (attacker == null || attacker instanceof Player) {
            return;
        }

        if (attacker.equals(npc.getEntity())) {
            return;
        }

        // Buscamos al dueño online para poder ejecutar el selector y el comando de salto si es un guerrero
        Player owner = getOwnerOf(npc);

        // Derivamos al mánager de combate
        new CompanionCombatManager(plugin).executeAttack(npc, attacker, owner);
    }

    private boolean isCompanion(NPC npc) {
        if (!plugin.getDataManager().getData().contains("players")) {
            return false;
        }
        for (String uuid : plugin.getDataManager().getData().getConfigurationSection("players").getKeys(false)) {
            int npcId = plugin.getDataManager().getData().getInt("players." + uuid + ".npc-id");
            if (npc.getId() == npcId) {
                return true;
            }
        }
        return false;
    }

    private Player getOwnerOf(NPC npc) {
        if (!plugin.getDataManager().getData().contains("players")) {
            return null;
        }
        for (String uuid : plugin.getDataManager().getData().getConfigurationSection("players").getKeys(false)) {
            int npcId = plugin.getDataManager().getData().getInt("players." + uuid + ".npc-id");
            if (npc.getId() == npcId) {
                return org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(uuid));
            }
        }
        return null;
    }

    private LivingEntity getRealAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }
}