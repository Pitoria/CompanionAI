package me.bray.companionai.commands;

import me.bray.companionai.CompanionAI;
import me.bray.companionai.utils.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SitTrait;
import net.citizensnpcs.trait.SleepTrait;
import net.citizensnpcs.trait.FollowTrait;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CompanionGestureCommand {

    private final CompanionAI plugin;

    public CompanionGestureCommand(CompanionAI plugin) {
        this.plugin = plugin;
    }

    private NPC getPlayerCompanion(Player player) {
        String path = getNpcPath(player.getUniqueId());

        // Buscamos la existencia del ID en data.yml usando el DataManager
        if (!plugin.getDataManager().getData().contains(path)) {
            return null;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        return CitizensAPI.getNPCRegistry().getById(npcId);
    }

    private String getNpcPath(UUID uuid) {
        return "players." + uuid + ".npc-id";
    }

    public void handle(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-gesture-help", null, player));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "sit" -> sit(player);
            case "sleep" -> sleep(player);
            case "sneak" -> sneak(player);
            case "stop" -> stop(player);
            default -> player.sendMessage(MessageUtil.msg(plugin, "companion-gesture-help", null, player));
        }
    }

    // Logica de sentarse
    private void sit(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        String sitPath = "players." + player.getUniqueId() + ".sitting";

        boolean alreadySitting = plugin.getDataManager().getData().getBoolean(sitPath, false);

        if (alreadySitting) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-sit-already", npc, player));
            return;
        }

        SitTrait sitTrait = npc.getOrAddTrait(SitTrait.class);
        sitTrait.setSitting(npc.getEntity().getLocation());

        plugin.getDataManager().getData().set(sitPath, true);
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin, "companion-sit-succes", npc, player));
    }

    // Logica de acostarse
    private void sleep(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        SleepTrait sleepTrait = npc.getOrAddTrait(SleepTrait.class);
        sleepTrait.setSleeping(npc.getEntity().getLocation());

        FollowTrait followTrait = npc.getTraitNullable(FollowTrait.class);
        if (followTrait != null) {
            followTrait.follow(null);
        }

        plugin.getDataManager().getData().set("players." + player.getUniqueId() + ".state", "STAY");
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin, "companion-sleep-succes", npc, player));
    }

    // Logica de agacharse
    private void sneak(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        if (npc.getEntity() instanceof Player npcPlayer) {
            npcPlayer.setSneaking(true);
        }

        player.sendMessage(MessageUtil.msg(plugin, "companion-sneak-succes", npc, player));
    }

    // Logica de STOP todas las GESTURES
    private void stop(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        SitTrait sitTrait = npc.getTraitNullable(SitTrait.class);

        if (sitTrait != null) {
            sitTrait.setSitting(null);
        }

        if (npc.getEntity() instanceof Player npcPlayer) {
            npcPlayer.setSneaking(false);
        }

        SleepTrait sleepTrait = npc.getTraitNullable(SleepTrait.class);

        if (sleepTrait != null) {
            sleepTrait.setSleeping(null);
        }

        String sitPath = "players." + player.getUniqueId() + ".sitting";
        plugin.getDataManager().getData().set(sitPath, false);
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin, "companion-stop-succes", npc, player));
    }
}