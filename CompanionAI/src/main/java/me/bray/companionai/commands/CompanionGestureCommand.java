package me.bray.companionai.commands;

import me.bray.companionai.CompanionAI;
import me.bray.companionai.utils.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
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

    private void sit(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        CitizensAPI.getDefaultNPCSelector().select(player, npc);

        boolean wasOp = player.isOp();
        try {
            player.setOp(true);
            player.performCommand("npc sitting --explicit true");
        } finally {
            player.setOp(wasOp);
        }

        player.sendMessage(MessageUtil.msg(plugin, "companion-sit-succes", npc, player));
    }

    private void sleep(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        CitizensAPI.getDefaultNPCSelector().select(player, npc);

        boolean wasOp = player.isOp();
        try {
            player.setOp(true);
            player.performCommand("npc panimate SLEEP");
        } finally {
            player.setOp(wasOp);
        }

        player.sendMessage(MessageUtil.msg(plugin, "companion-sleep-succes", npc, player));
    }

    private void sneak(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        CitizensAPI.getDefaultNPCSelector().select(player, npc);

        boolean wasOp = player.isOp();
        try {
            player.setOp(true);
            player.performCommand("npc panimate SNEAK");
        } finally {
            player.setOp(wasOp);
        }

        player.sendMessage(MessageUtil.msg(plugin, "companion-sneak-succes", npc, player));
    }

    private void stop(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        CitizensAPI.getDefaultNPCSelector().select(player, npc);

        boolean wasOp = player.isOp();
        try {
            player.setOp(true);
            player.performCommand("npc panimate STOP_SLEEPING");
            player.performCommand("npc panimate STOP_SNEAKING");
            player.performCommand("npc sitting --explicit false");
        } finally {
            player.setOp(wasOp);
        }

        player.sendMessage(MessageUtil.msg(plugin, "companion-stop-succes", npc, player));
    }
}