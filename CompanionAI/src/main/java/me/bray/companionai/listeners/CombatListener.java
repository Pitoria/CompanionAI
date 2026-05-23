package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI; // Asegúrate de importar tu clase principal
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final CompanionAI plugin;

    public CombatListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        // Detectar cualquier proyectil
        if (event.getDamager() instanceof Projectile projectile && projectile.hasMetadata("MagicArrow")) {

            if (event.getEntity() instanceof LivingEntity victim) {
                Location hitLoc = projectile.getLocation();

                // 1. Lógica del MAGO DE HIELO
                if (projectile.hasMetadata("IceArrow")) {
                    double damage = plugin.getConfig().getDouble("combat.ice-damage", 4.0);
                    victim.damage(damage); // Daño manual a la víctima
                    victim.setFreezeTicks(200);
                    hitLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, hitLoc, 20, 0.5, 0.5, 0.5, 0.1);
                    hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

                    // 2. Lógica del MAGO DE FUEGO
                } else if (projectile.hasMetadata("FireArrow")) {

                    victim.setFireTicks(100);
                    hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 20, 0.5, 0.5, 0.5, 0.1);
                    // 3. Lógica del MAGO DE AIRE
                } else if (projectile.hasMetadata("WindArrow")) {
                    double damage = plugin.getConfig().getDouble("combat.wind-damage", 8.5);
                    victim.damage(damage);

                    // efecto de empuje
                    victim.setVelocity(projectile.getVelocity().multiply(0.5));
                    hitLoc.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 10, 0.2, 0.2, 0.2, 0.1);
                }


                // Explosión visual común
                hitLoc.getWorld().createExplosion(hitLoc, 0.0F, false, false);
            }
        }
    }
}