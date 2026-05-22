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
        // 1. Primero detectamos al jugador (aquí se crea la variable 'player')
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 2. Buscamos al atacante real
        LivingEntity attacker = getRealAttacker(event);

        // 3. VALIDACIÓN: Si no hay atacante o si eres tú mismo, ignorar
        if (attacker == null || attacker.equals(player)) {
            return;
        }

        // 4. El resto de tu lógica para buscar el NPC de este 'player'
        String path = "players." + player.getUniqueId() + ".npc-id";
        if (!plugin.getDataManager().getData().contains(path)) {
            return;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);

        if (npc != null && npc.isSpawned()) {
            new CompanionCombatManager(plugin).executeAttack(npc, attacker, player);
        }
    }

    // 2. SI LO ATACAN A ÉL
    @EventHandler
    public void onCompanionDamage(EntityDamageByEntityEvent event) {
        // 1. Primero obtenemos el NPC que recibió el golpe (aquí se crea 'npc')
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getEntity());
        if (npc == null || !isCompanion(npc)) {
            return;
        }

        // 2. Buscamos al atacante y al dueño del NPC (aquí se crean 'attacker' y 'owner')
        LivingEntity attacker = getRealAttacker(event);
        Player owner = getOwnerOf(npc);

        // 3. VALIDACIÓN: Si el atacante es nulo o es el dueño pegándole a su propio NPC, ignorar
        if (attacker == null || (owner != null && attacker.equals(owner))) {
            return;
        }

        // 4. Si pasa los filtros, el NPC contraataca defendiendo a su dueño
        if (owner != null) {
            new CompanionCombatManager(plugin).executeAttack(npc, attacker, owner);
        }
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