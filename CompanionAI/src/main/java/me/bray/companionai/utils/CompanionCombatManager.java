package me.bray.companionai.utils;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.Map;

public class CompanionCombatManager {

    private final CompanionAI plugin;
    private static final Map<Integer, BukkitRunnable> activeCombatTasks = new HashMap<>();
    private static final Material BREEZE_ROD_MAT = Material.matchMaterial("BREEZE_ROD");

    public CompanionCombatManager(CompanionAI plugin) {
        this.plugin = plugin;
    }

    public void executeAttack(NPC npc, LivingEntity target, Player owner) {
        if (npc == null || !npc.isSpawned() || target == null || target.isDead() || !target.isValid()) return;

        int npcId = npc.getId();
        if (activeCombatTasks.containsKey(npcId)) return;

        ItemStack handItem = npc.getEntity() instanceof LivingEntity living ? living.getEquipment().getItemInMainHand() : null;
        Material type = (handItem != null) ? handItem.getType() : Material.AIR;
        boolean isBreezeRod = (BREEZE_ROD_MAT != null && type == BREEZE_ROD_MAT);

        long delay = 40L;

        BukkitRunnable combatTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (npc == null || !npc.isSpawned() || target == null || target.isDead()) {
                    this.cancel();
                    activeCombatTasks.remove(npcId);
                    return;
                }

                LivingEntity npcPlayer = (LivingEntity) npc.getEntity();
                Vector direction = target.getLocation().toVector().subtract(npcPlayer.getLocation().toVector()).normalize();

                // Lógica de ataque

                // Arquero/Ballesta

                if (type == Material.BOW || type == Material.CROSSBOW) {
                    Arrow arrow = npcPlayer.launchProjectile(Arrow.class);
                    arrow.setShooter(npcPlayer);
                    arrow.setVelocity(direction.multiply(3.8));
                    arrow.setDamage(arrow.getDamage() * getDamageFor("archer-damage", 1.5));
                    arrow.setKnockbackStrength(2);
                    arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

                    // Mago de fuego

                } else if (type == Material.BLAZE_ROD) {
                    Arrow arrow = npcPlayer.launchProjectile(Arrow.class);
                    arrow.setShooter(npcPlayer);
                    arrow.setVelocity(direction.multiply(3.5));
                    arrow.setDamage(arrow.getDamage() * getDamageFor("fire-damage", 2.0));
                    arrow.setFireTicks(1200);
                    arrow.setMetadata("MagicArrow", new FixedMetadataValue(plugin, true));
                    arrow.setKnockbackStrength(2);
                    arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

                    // Mago de hielo

                } else if (type == Material.END_ROD) {
                    Arrow arrow = npcPlayer.launchProjectile(Arrow.class);
                    arrow.setShooter(npcPlayer);
                    arrow.setVelocity(direction.multiply(3.4));
                    arrow.setDamage(arrow.getDamage() * getDamageFor("ice-damage", 1.6));
                    arrow.setMetadata("IceArrow", new FixedMetadataValue(plugin, true));
                    arrow.setMetadata("MagicArrow", new FixedMetadataValue(plugin, true));
                    arrow.setKnockbackStrength(3);
                    arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

                    // Mago de viento

                } else if (isBreezeRod) {
                    try {
                        Class<?> wcClass = Class.forName("org.bukkit.entity.WindCharge");
                        Projectile wc = npcPlayer.launchProjectile((Class<? extends Projectile>) wcClass);
                        wc.setShooter(npcPlayer);
                        wc.setVelocity(direction.multiply(3.5));
                    } catch (Exception e) {
                        Arrow windArrow = npcPlayer.launchProjectile(Arrow.class);
                        windArrow.setShooter(npcPlayer);
                        windArrow.setVelocity(direction.multiply(2.0));
                        windArrow.setDamage(windArrow.getDamage() * getDamageFor("wind-damage", 1.5));
                        windArrow.setKnockbackStrength(4);
                        windArrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    }
                }
            }
        };

        activeCombatTasks.put(npcId, combatTask);
        combatTask.runTaskTimer(plugin, 0L, delay);
    }

    private double getDamageFor(String path, double defaultValue) {
        return plugin.getConfig().getDouble("combat." + path, defaultValue);
    }
}