package me.bray.companionai.listeners;

import me.bray.companionai.CompanionAI; // Asegúrate de importar tu clase principal
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final CompanionAI plugin;

    // ESTE ES EL CONSTRUCTOR QUE FALTA
    public CombatListener(CompanionAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        // Verificamos si es una flecha mágica
        if (event.getDamager() instanceof Arrow arrow && arrow.hasMetadata("MagicArrow")) {
            Location hitLoc = arrow.getLocation();

            // Explosión visual/daño base (opcional, igual que tenías)
            hitLoc.getWorld().createExplosion(hitLoc, 0.0F, false, false);
            hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, hitLoc, 1);

            if (event.getEntity() instanceof LivingEntity victim) {

                // --- DIFERENCIACIÓN: ¿Es Hielo o es Fuego? ---

                if (arrow.hasMetadata("IceArrow")) {
                    // Lógica del Mago de Hielo (END_ROD)
                    victim.setFreezeTicks(200); // 10 segundos de congelamiento (200 ticks)
                    hitLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, hitLoc, 20, 0.5, 0.5, 0.5, 0.1);
                    hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

                } else {
                    // Lógica del Mago de Fuego (BLAZE_ROD)
                    victim.setFireTicks(100); // 5 segundos de fuego
                    hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 20, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }
}