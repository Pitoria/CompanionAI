package me.bray.companionai.commands;

import me.bray.companionai.CompanionAI;
import me.bray.companionai.utils.CompanionNameUtil;
import me.bray.companionai.utils.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.editor.EquipmentEditor;
import net.citizensnpcs.trait.FollowTrait;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CompanionCommand implements CommandExecutor, TabCompleter {

    private final CompanionAI plugin;
    private final CompanionGestureCommand gestureCommand;

    public CompanionCommand(CompanionAI plugin) {
        this.plugin = plugin;
        this.gestureCommand = new CompanionGestureCommand(plugin);
    }

    //================================TabComplete/Permisos=====================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return Arrays.asList(
                    "create",
                    "remove",
                    "summon",
                    "follow",
                    "stay",
                    "info",
                    "reload",
                    "rename",
                    "skin",
                    "equip",
                    "pickup",
                    "gesture"
            );
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("gesture")) {
            return Arrays.asList(
                    "sit",
                    "sleep",
                    "sneak",
                    "stop"
            );
        }

        if (args.length == 2 &&
                (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("remove"))) {

            return org.bukkit.Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .toList();
        }

        return Collections.emptyList();
    }

    //================================Comandos/Permisos=====================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0) {
            if (player != null) {
                sendUsage(player);
            } else {
                sender.sendMessage(MessageUtil.msg(plugin, "command-use", null, player));
            }
            return true;
        }

        boolean isAdminAction =
                args[0].equalsIgnoreCase("reload") ||
                        (args.length >= 2 && (
                                args[0].equalsIgnoreCase("create") ||
                                        args[0].equalsIgnoreCase("remove")
                        ));

        if (isAdminAction) {
            if (!sender.hasPermission("companion.admin")) {
                sender.sendMessage(MessageUtil.msg(plugin, "no-permission", null, player));
                return true;
            }
        } else {
            if (player == null) {
                sender.sendMessage(MessageUtil.msg(plugin, "only-player", null, player));
                return true;
            }

            if (!player.hasPermission("companion.use")) {
                sender.sendMessage(MessageUtil.msg(plugin, "no-permission", null, player));
                return true;
            }
        }

        switch (args[0].toLowerCase()) {

            case "gesture" -> gestureCommand.handle(player, args);

            case "reload" -> reloadPlugin(sender);

            case "create" -> {

                if (args.length >= 2 || player == null) {

                    if (!sender.hasPermission("companion.admin")) {
                        sender.sendMessage(MessageUtil.msg(plugin, "no-permission", null, player));
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(MessageUtil.msg(plugin, "player-not-found", null, player));
                        return true;
                    }

                    createCompanion(target);
                    sender.sendMessage(MessageUtil.msg(plugin, "companion-created", null, player));
                    return true;
                }

                if (!player.hasPermission("companion.admin")) {
                    sender.sendMessage(MessageUtil.msg(plugin, "no-permission", null, player));
                    return true;
                }

                createCompanion(player);
            }

            case "remove" -> {

                if (args.length >= 2 || player == null) {

                    if (!sender.hasPermission("companion.admin")) {
                        sender.sendMessage(MessageUtil.msg(plugin, "no-permission", null, player));
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(MessageUtil.msg(plugin, "player-not-found", null, player));
                        return true;
                    }

                    removeCompanion(target);
                    sender.sendMessage(MessageUtil.msg(plugin, "companion-removed", null, player));
                    return true;
                }

                if (!player.hasPermission("companion.admin")) {
                    player.sendMessage(MessageUtil.msg(plugin,"no-permission", null, player));
                    return true;
                }

                removeCompanion(player);
            }

            case "summon" -> summonCompanion(player);
            case "follow" -> followCompanion(player);
            case "stay" -> stayCompanion(player);
            case "info" -> showInfo(player);
            case "rename" -> renameCompanion(player, args);
            case "skin" -> changeSkin(player, args);
            case "equip" -> openEquipment(player);
            case "pickup" -> togglePickup(player);
            default -> sendUsage(player);
        }

        return true;
    }

    //================================Reloads=====================================================

    private void reloadPlugin(CommandSender sender) {
        plugin.reloadConfig();
        plugin.reloadMessages();
        sender.sendMessage(MessageUtil.msg(plugin,"plugin-reload", null, null));
    }

    //================================CreateCommand=====================================================

    private void createCompanion(Player player) {
        String path = getNpcPath(player.getUniqueId());

        if (plugin.getDataManager().getData().contains(path)) {
            NPC npc = getPlayerCompanion(player);
            player.sendMessage(MessageUtil.msg(plugin,"companion-limit", npc, player));
            return;
        }

        // NombreNPC

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(
                EntityType.PLAYER,
                player.getName() + " NPC"
        );
        npc.data().setPersistent(
                "companion-base-name",
                player.getName() + " NPC"
        );

        // SpawnNPC

        npc.spawn(player.getLocation());
        applyStats(npc);

        // Actualizacion de vida para el nametag

        if (plugin.getConfig().getBoolean("settings.health-name.enabled", true)) {
            CompanionNameUtil.updateHealthName(plugin, npc);
        }

        // Establece propiedades del NPC

        FollowTrait followTrait = npc.getOrAddTrait(FollowTrait.class);
        followTrait.setProtect(true);

        npc.setProtected(false);
        npc.data().set(NPC.Metadata.DEFAULT_PROTECTED, false);

        // Asigna vida desde la config.yml

        if (npc.getEntity() instanceof org.bukkit.entity.Player npcPlayer) {
            npcPlayer.setGameMode(org.bukkit.GameMode.SURVIVAL);

            double maxHealth = plugin.getConfig().getDouble("settings.stats.max-health", 40.0);
            double attackDamage = plugin.getConfig().getDouble("settings.stats.attack-damage", 8.0);

            var health = npcPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);

            if (health != null) {
                health.setBaseValue(maxHealth);
            }

            npcPlayer.setHealth(maxHealth);

            var damage = npcPlayer.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);

            if (damage != null) {
                damage.setBaseValue(attackDamage);
            }

        }

        plugin.getDataManager().getData().set(getNpcPath(player.getUniqueId()), npc.getId());
        plugin.getDataManager().getData().set(getStatePath(player.getUniqueId()), "STAY");
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin,"companion-created-succes", npc, player));
        player.sendMessage(ChatColor.GRAY + "NPC ID: " + npc.getId());
    }

    //================================ComandoRemove=====================================================

    private void removeCompanion(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found", null, player));
            return;
        }

        npc.destroy();

        plugin.getDataManager().getData().set("players." + player.getUniqueId(), null);
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin,"companion-removed-succes", npc, player));
    }

    //================================ComandoSummon=====================================================

    private void summonCompanion(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found", null, player));
            return;
        }

        if (!npc.isSpawned()) {
            npc.spawn(player.getLocation());
            applyStats(npc);
        } else {
            npc.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            applyStats(npc);
        }

        npc.setProtected(false);
        npc.data().set(NPC.Metadata.DEFAULT_PROTECTED, false);

        player.sendMessage(MessageUtil.msg(plugin,"companion-invoked", npc, player));
    }

    //================================ComandoFollow=====================================================

    private void followCompanion(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found", null, player));
            return;
        }

        FollowTrait followTrait = npc.getOrAddTrait(FollowTrait.class);
        followTrait.follow(player);
        followTrait.setFollowingMargin(3.0);
        followTrait.setProtect(true);

        plugin.getDataManager().getData().set(getStatePath(player.getUniqueId()), "FOLLOW");
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin,"companion-follow-enabled", npc, player));
    }

    //================================ComandoStay=====================================================

    private void stayCompanion(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found", null, player));
            return;
        }

        FollowTrait followTrait = npc.getTraitNullable(FollowTrait.class);

        if (followTrait != null) {
            followTrait.follow(null);
        }

        plugin.getDataManager().getData().set(getStatePath(player.getUniqueId()), "STAY");
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin,"companion-follow-disabled", npc, player));
    }

    //================================ComandoShowInfo=====================================================

    private void showInfo(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found", null, player));
            return;
        }

        String state = plugin.getDataManager().getData().getString(
                getStatePath(player.getUniqueId()),
                "UNKNOWN"
        );

        if (npc.getEntity() instanceof Player npcPlayer) {
            player.sendMessage(
                    ChatColor.YELLOW + "Health: " +
                            ChatColor.RED + (int) Math.ceil(npcPlayer.getHealth()) +
                            ChatColor.WHITE + "/" +
                            ChatColor.RED + (int) npcPlayer.getMaxHealth()
            );
        }

        player.sendMessage(MessageUtil.msg(plugin,"companion-info-header", npc, player));
        player.sendMessage(MessageUtil.msg(plugin,"companion-info-name", npc, player));
        player.sendMessage(MessageUtil.msg(plugin,"companion-info-id", npc, player));
        player.sendMessage(MessageUtil.msg(plugin,"companion-info-state", npc, player));
        player.sendMessage(MessageUtil.msg(plugin,"companion-info-spawned", npc, player));
    }

    //================================ComandoRename=====================================================

    private void renameCompanion(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-rename-help", null, player));
            return;
        }

        NPC npc = getPlayerCompanion(player);
        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        // Juntamos los argumentos para permitir nombres con espacios (ej: &bMi &aNPC)
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Guardamos el nombre base con los '&' originales para no perder los colores
        npc.data().setPersistent("companion-base-name", name);

        // Esto lee el 'health-format' de config.yml y actualiza el nametag al instante
        CompanionNameUtil.updateHealthName(plugin, npc);

        player.sendMessage(MessageUtil.msg(plugin, "companion-rename-succes", npc, player));
    }

    //================================ComandoSkin=============================================

    private void changeSkin(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-skin-help", null, player));
            return;
        }

        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found",null, player));
            return;
        }

        String skinName = args[1];

        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinName(skinName);

        plugin.getDataManager().getData().set(
                getSkinPath(player.getUniqueId()),
                skinName
        );
        plugin.getDataManager().save();

        npc.despawn();
        npc.spawn(player.getLocation());
        applyStats(npc);

        player.sendMessage(MessageUtil.msg(plugin,"companion-skin-succes", npc, player));
    }

    //================================ComandoEquip=====================================================

    private void openEquipment(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found",null, player));
            return;
        }

        EquipmentEditor editor = new EquipmentEditor(player, npc);
        plugin.getServer().getPluginManager().registerEvents(editor, plugin);
        editor.begin();
    }

    //================================ComandoPickup=====================================================

    private void togglePickup(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin,"companion-not-found", null, player));
            return;
        }

        boolean current = plugin.getDataManager().getData().getBoolean(
                getPickupPath(player.getUniqueId()),
                false
        );

        boolean next = !current;

        plugin.getDataManager().getData().set(getPickupPath(player.getUniqueId()), next);
        plugin.getDataManager().save();

        CitizensAPI.getDefaultNPCSelector().select(player, npc);

        boolean wasOp = player.isOp();

        try {
            player.setOp(true);
            player.performCommand("npc pickupitems --set " + next);
        } finally {
            player.setOp(wasOp);
        }

        player.sendMessage(
                next
                        ? MessageUtil.msg(plugin,"pickup-enabled", npc, player)
                        : MessageUtil.msg(plugin,"pickup-disabled", npc, player)
        );
    }

    private String getPickupPath(UUID uuid) {
        return "players." + uuid + ".pickup-items";
    }

    //================================Vida/Daño>Config.yml=====================================================

    private void applyStats(NPC npc) {
        if (!(npc.getEntity() instanceof Player npcPlayer)) {
            return;
        }

        npcPlayer.setGameMode(GameMode.SURVIVAL);

        double maxHealth = plugin.getConfig().getDouble("settings.stats.max-health", 40.0);
        double attackDamage = plugin.getConfig().getDouble("settings.stats.attack-damage", 8.0);

        var health = npcPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (health != null) {
            health.setBaseValue(maxHealth);
        }

        npcPlayer.setHealth(Math.min(npcPlayer.getHealth(), maxHealth));

        var damage = npcPlayer.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);

        if (damage != null) {
            damage.setBaseValue(attackDamage);
        }
    }

    //================================GuardaDatos=====================================================

    private NPC getPlayerCompanion(Player player) {
        String path = getNpcPath(player.getUniqueId());

        if (!plugin.getDataManager().getData().contains(path)) {
            return null;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        return CitizensAPI.getNPCRegistry().getById(npcId);
    }

    private String getNpcPath(UUID uuid) {
        return "players." + uuid + ".npc-id";
    }

    private String getStatePath(UUID uuid) {
        return "players." + uuid + ".state";
    }

    private String getSkinPath(UUID uuid) {
        return "players." + uuid + ".skin";
    }

    //================================CommandList=====================================================

    private void sendUsage(Player player) {

            player.sendMessage(ChatColor.RED + "/companion create [player_name]");
            player.sendMessage(ChatColor.RED + "/companion remove [player_name]");
            player.sendMessage(ChatColor.RED + "/companion summon");
            player.sendMessage(ChatColor.RED + "/companion follow");
            player.sendMessage(ChatColor.RED + "/companion stay");
            player.sendMessage(ChatColor.RED + "/companion info");
            player.sendMessage(ChatColor.RED + "/companion rename");
            player.sendMessage(ChatColor.RED + "/companion skin");
            player.sendMessage(ChatColor.RED + "/companion equip");
            player.sendMessage(ChatColor.RED + "/companion pickup");
            player.sendMessage(ChatColor.RED + "/companion gesture [sneak, sit, sleep]");
    }
}