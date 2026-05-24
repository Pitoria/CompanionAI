package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.FollowTrait;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class CompanionWorldListener implements Listener {

    private final CompanionAI plugin;

    public CompanionWorldListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        NPC npc = getPlayerCompanion(player);

        if (npc == null || !npc.isSpawned()) {
            return;
        }

        String state = plugin.getDataManager().getData().getString(
                "players." + player.getUniqueId() + ".state",
                "STAY"
        );

        if (!state.equalsIgnoreCase("FOLLOW")) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (npc.isSpawned()) {
                npc.despawn();
            }

            npc.spawn(player.getLocation());

            FollowTrait followTrait = npc.getOrAddTrait(FollowTrait.class);
            followTrait.follow(player);
            followTrait.setFollowingMargin(3.0);
            followTrait.setProtect(false);

            makeVulnerable(npc);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (npc.isSpawned()) {
                    makeVulnerable(npc);
                }
            }, 2L);

        }, 5L);
    }

    private void makeVulnerable(NPC npc) {
        npc.setProtected(false);
        npc.data().set(NPC.Metadata.DEFAULT_PROTECTED, false);
        npc.data().set("protected", false);

        if (npc.getEntity() instanceof org.bukkit.entity.LivingEntity entity) {
            entity.setInvulnerable(false);
            entity.setNoDamageTicks(0);
        }

        if (npc.getEntity() instanceof org.bukkit.entity.Player npcPlayer) {
            npcPlayer.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
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