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

        // ARREGLO 1: Si ya estaba atacando, cancelamos la tarea vieja para ir a por el nuevo objetivo
        if (activeCombatTasks.containsKey(npcId)) {
            activeCombatTasks.get(npcId).cancel();
            activeCombatTasks.remove(npcId);
        }

        ItemStack handItem = npc.getEntity() instanceof LivingEntity living ? living.getEquipment().getItemInMainHand() : null;
        Material type = (handItem != null) ? handItem.getType() : Material.AIR;
        boolean isBreezeRod = (BREEZE_ROD_MAT != null && type == BREEZE_ROD_MAT);

        boolean isRangedWeapon = type == Material.BOW
                || type == Material.CROSSBOW
                || type == Material.BLAZE_ROD
                || type == Material.END_ROD
                || isBreezeRod;

        if (!isRangedWeapon) {
            npc.getNavigator().setTarget(target, true);
            return;
        }

        // DELAYS
        long delay = 40L;
        if (type == Material.BOW || type == Material.CROSSBOW) {
            delay = plugin.getConfig().getLong("combat-delay.archer", 40L);
        } else if (type == Material.BLAZE_ROD) {
            delay = plugin.getConfig().getLong("combat-delay.fire", 30L);
        } else if (isBreezeRod) {
            delay = plugin.getConfig().getLong("combat-delay.wind-elementalist", 20L);
        } else if (type == Material.END_ROD) {
            delay = plugin.getConfig().getLong("combat-delay.ice-mage", 50L);
        }

        BukkitRunnable combatTask = new BukkitRunnable() {
            @Override
            public void run() {
                // ARREGLO 2: Validación dentro del ciclo para detenerlo si el mob desaparece
                if (npc == null || !npc.isSpawned() || target == null || target.isDead() || !target.isValid()) {
                    this.cancel();
                    activeCombatTasks.remove(npcId);
                    return;
                }

                LivingEntity npcPlayer = (LivingEntity) npc.getEntity();

                // Evitar error matemático al calcular vectores
                if (target.getLocation().distanceSquared(npcPlayer.getLocation()) < 0.1) return;

                Vector direction = target.getLocation().toVector().subtract(npcPlayer.getLocation().toVector()).normalize();

                // LÓGICA DE ATAQUE A DISTANCIA
                if (type == Material.BOW || type == Material.CROSSBOW) {
                    Arrow arrow = npcPlayer.launchProjectile(Arrow.class);
                    arrow.setShooter(npcPlayer);
                    arrow.setVelocity(direction.multiply(3.8));
                    arrow.setDamage(arrow.getDamage() * getDamageFor("archer-damage", 1.5));
                    arrow.setKnockbackStrength(2);
                    arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

                } else if (type == Material.BLAZE_ROD) {
                    Arrow arrow = npcPlayer.launchProjectile(Arrow.class);
                    arrow.setShooter(npcPlayer);
                    arrow.setVelocity(direction.multiply(1.5));
                    arrow.setDamage(arrow.getDamage() * getDamageFor("fire-damage", 2.0));
                    arrow.setFireTicks(1200);
                    arrow.setMetadata("MagicArrow", new FixedMetadataValue(plugin, true));
                    arrow.setMetadata("FireArrow", new FixedMetadataValue(plugin, true));
                    arrow.setKnockbackStrength(2);
                    arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

                } else if (type == Material.END_ROD) {
                    Snowball snowball = npcPlayer.launchProjectile(Snowball.class);
                    snowball.setShooter(npcPlayer);
                    snowball.setVelocity(direction.multiply(1.6));
                    snowball.setMetadata("MagicArrow", new FixedMetadataValue(plugin, true));
                    snowball.setMetadata("IceArrow", new FixedMetadataValue(plugin, true));
                    npcPlayer.getWorld().playSound(npcPlayer.getLocation(), org.bukkit.Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);

                } else if (isBreezeRod) {
                    try {
                        Class<?> wcClass = Class.forName("org.bukkit.entity.WindCharge");
                        Projectile wc = npcPlayer.launchProjectile((Class<? extends Projectile>) wcClass);
                        wc.setShooter(npcPlayer);
                        wc.setVelocity(direction.multiply(4.5));
                        wc.setMetadata("MagicArrow", new FixedMetadataValue(plugin, true));
                        wc.setMetadata("WindArrow", new FixedMetadataValue(plugin, true));
                    } catch (Exception e) {
                        Arrow windArrow = npcPlayer.launchProjectile(Arrow.class);
                        windArrow.setShooter(npcPlayer);
                        windArrow.setVelocity(direction.multiply(4.0));
                        windArrow.setDamage(windArrow.getDamage() * getDamageFor("wind-damage", 1.5));
                        windArrow.setKnockbackStrength(4);
                        windArrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    }

                    // ARREGLO 3: LÓGICA DE ATAQUE CUERPO A CUERPO (Melee)
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