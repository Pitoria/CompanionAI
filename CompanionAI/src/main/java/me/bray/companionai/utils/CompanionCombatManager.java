package me.bray.companionai.utils;

import me.bray.companionai.CompanionAI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
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

        // ==========================================================
        // 1. DETERMINAR EL DELAY (VELOCIDAD) SEGÚN LA CONFIG
        // ==========================================================
        long delay = 40L; // Default
        if (type == Material.BOW) {
            delay = plugin.getConfig().getLong("combat-delay.archer", 40L);
        } else if (type == Material.BLAZE_ROD) {
            delay = plugin.getConfig().getLong("combat-delay.mage", 40L);
        } else if (isBreezeRod) {
            delay = plugin.getConfig().getLong("combat-delay.wind-elementalist", 40L);
        }

        if (type != Material.BOW && type != Material.BLAZE_ROD && !isBreezeRod) {
            npc.getNavigator().setTarget(target, true);
            return;
        }

        BukkitRunnable combatTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!npc.isSpawned() || target.isDead() || !target.isValid()) {
                    activeCombatTasks.remove(npcId);
                    this.cancel();
                    return;
                }

                if (npc.getEntity().getLocation().distance(target.getLocation()) > 20.0) {
                    activeCombatTasks.remove(npcId);
                    this.cancel();
                    return;
                }

                if (npc.getNavigator().isNavigating()) npc.getNavigator().cancelNavigation();
                Location npcLoc = npc.getEntity().getLocation();
                npcLoc.setDirection(target.getLocation().toVector().subtract(npcLoc.toVector()));
                npc.getEntity().teleport(npcLoc);

                if (npc.getEntity() instanceof Player npcPlayer) {
                    Vector direction = target.getEyeLocation().toVector().subtract(npcPlayer.getEyeLocation().toVector()).normalize();

                    if (type == Material.BOW) {
                        Arrow arrow = npcPlayer.launchProjectile(Arrow.class);
                        arrow.setShooter(npcPlayer);
                        arrow.setVelocity(direction.multiply(1.8));
                        arrow.setKnockbackStrength(2);
                        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    } else if (type == Material.BLAZE_ROD) {
                        Arrow arrow = npcPlayer.launchProjectile(Arrow.class);
                        arrow.setShooter(npcPlayer);
                        arrow.setVelocity(direction.multiply(1.8));
                        arrow.setFireTicks(1200);
                        arrow.setKnockbackStrength(2);
                        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    } else if (isBreezeRod) {
                        try {
                            Class<?> wcClass = Class.forName("org.bukkit.entity.WindCharge");
                            Projectile wc = npcPlayer.launchProjectile((Class<? extends Projectile>) wcClass);
                            wc.setShooter(npcPlayer);
                            wc.setVelocity(direction.multiply(1.5));
                        } catch (Exception e) {
                            Arrow windArrow = npcPlayer.launchProjectile(Arrow.class);
                            windArrow.setShooter(npcPlayer);
                            windArrow.setVelocity(direction.multiply(2.0));
                            windArrow.setKnockbackStrength(4);
                            windArrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                        }
                    }
                }
            }
        };

        activeCombatTasks.put(npcId, combatTask);
        // ==========================================================
        // 2. USAR EL DELAY AQUÍ ABAJO
        // ==========================================================
        combatTask.runTaskTimer(plugin, 0L, delay);
    }
}