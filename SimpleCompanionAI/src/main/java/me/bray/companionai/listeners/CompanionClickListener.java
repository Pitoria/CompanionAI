package me.bray.companionai.listeners;

import me.bray.companionai.utils.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.editor.EquipmentEditor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.FollowTrait;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.citizensnpcs.api.trait.trait.Inventory;

import java.util.UUID;

public class CompanionClickListener implements Listener {

    private final CompanionAI plugin;

    public CompanionClickListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        // La configuración general sí se lee desde el config.yml
        if (!plugin.getConfig().getBoolean("settings.clicks.enabled", true)) {
            return;
        }

        Player player = event.getClicker();
        NPC npc = event.getNPC();

        if (!isOwner(player, npc)) {
            return;
        }

        if (player.isSneaking()) {

            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_CHEST_OPEN,
                    1.0f,
                    1.0f
            );

            Inventory inventory = npc.getOrAddTrait(Inventory.class);
            inventory.openInventory(player);
            return;
        }

        toggleFollow(player, npc);
    }

    @EventHandler
    public void onLeftClick(NPCDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        NPC npc = event.getNPC();

        if (!isOwner(player, npc)) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        event.setCancelled(true);

        player.playSound(
                player.getLocation(),
                org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC,
                1.0f,
                1.0f
        );

        openEquipment(player);
    }

    //================================TOGGLE FOLLOW===================================================

    private void toggleFollow(Player player, NPC npc) {
        String statePath = getStatePath(player.getUniqueId());
        // Se lee el estado desde el data.yml usando el DataManager
        String state = plugin.getDataManager().getData().getString(statePath, "STAY");

        if (state.equalsIgnoreCase("FOLLOW")) {
            FollowTrait followTrait = npc.getTraitNullable(FollowTrait.class);

            if (followTrait != null) {
                followTrait.follow(null);
            }

            // Guarda el estado en data.yml
            plugin.getDataManager().getData().set(statePath, "STAY");
            plugin.getDataManager().save();

            player.sendMessage(MessageUtil.msg(plugin, "companion-stay-click", npc, player));
            return;
        }

        FollowTrait followTrait = npc.getOrAddTrait(FollowTrait.class);
        followTrait.follow(player);
        followTrait.setFollowingMargin(3.0);
        followTrait.setProtect(false);

        // Guarda el estado en data.yml
        plugin.getDataManager().getData().set(statePath, "FOLLOW");
        plugin.getDataManager().save();

        player.sendMessage(MessageUtil.msg(plugin, "companion-follow-click", npc, player));
    }

    private boolean isOwner(Player player, NPC npc) {
        String path = getNpcPath(player.getUniqueId());

        // Comprueba la existencia en data.yml, no en config.yml
        if (!plugin.getDataManager().getData().contains(path)) {
            return false;
        }

        int npcId = plugin.getDataManager().getData().getInt(path);
        return npc.getId() == npcId;
    }

    //================================EQUIP===================================================

    private void openEquipment(Player player) {
        NPC npc = getPlayerCompanion(player);

        if (npc == null) {
            player.sendMessage(MessageUtil.msg(plugin, "companion-not-found", null, player));
            return;
        }

        EquipmentEditor editor = new EquipmentEditor(player, npc);
        plugin.getServer().getPluginManager().registerEvents(editor, plugin);
        editor.begin();
    }

    private NPC getPlayerCompanion(Player player) {
        String path = getNpcPath(player.getUniqueId());

        // Busca el ID correcto asignado en data.yml
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
}