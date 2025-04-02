// NAPISANE PRZEZ DC:@BOSS_OLUSH

package com.easyantylog.antylogout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler; 
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

public class AntyLogoutPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Long> combatTagged = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Set<String> blockedCommands = new HashSet<>(Arrays.asList("/spawn", "/home", "/casino", "/tpa", "/tpaccept", "/home1", "/home2", "/home3", "/home4", "/home5"));
    private final long COMBAT_DURATION = 15 * 1000;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AntyLogoutPlugin został włączony!");
    }

    @Override
    public void onDisable() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
        getLogger().info("AntyLogoutPlugin został wyłączony!");
    }
    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player attacked = (Player) event.getEntity();
        Player attacker = null;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }

        else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();

                ProjectileSource shooter = projectile.getShooter();
                if (!(shooter instanceof Player)) return;

                if (!(event.getDamager().getType().toString().equals("ARROW") ||
                    event.getDamager().getType().toString().equals("TRIDENT") ||
                    event.getDamager().getType().toString().equals("WIND_CHARGE"))) {
                    return;
                }
            }
        }

        if (attacker == null) return;

        long currentTime = System.currentTimeMillis();
        combatTagged.put(attacked.getUniqueId(), currentTime);
        combatTagged.put(attacker.getUniqueId(), currentTime);

        showCombatBossBar(attacked);
        showCombatBossBar(attacker);

        attacked.sendMessage(ChatColor.RED + "Jesteś w walce! Nie możesz się teleportować przez 15 sekund.");
        attacker.sendMessage(ChatColor.GREEN + "Zaatakowałeś gracza. Jesteś w walce przez 15 sekund.");
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
    }

    combatTagged.remove(uuid);
}


    private void showCombatBossBar(Player player) {
        final UUID uuid = player.getUniqueId();

        BossBar bar = bossBars.get(uuid);
        if (bar == null) {
            final BossBar newBar = Bukkit.createBossBar(ChatColor.RED + "WALKA! Odliczanie do końca: 15s", BarColor.RED, BarStyle.SOLID);
            newBar.addPlayer(player);
            bossBars.put(uuid, newBar);
            showCombatBossBar(player);
            return;
        }

        bar.setVisible(true);

        Bukkit.getScheduler().runTaskTimer(this, task -> {
            long elapsed = System.currentTimeMillis() - combatTagged.getOrDefault(uuid, 0L);
            double progress = 1.0 - ((double) elapsed / COMBAT_DURATION);
            if (progress <= 0 || !isInCombat(player)) {
                bar.setVisible(false);
                bar.removeAll();
                bossBars.remove(uuid);
                task.cancel();
                return;
            }

            int secondsLeft = (int) ((COMBAT_DURATION - elapsed) / 1000);
            bar.setTitle(ChatColor.RED + "WALKA! Odliczanie do końca: " + secondsLeft + "s");
            bar.setProgress(Math.max(0.0, progress));
        }, 0L, 20L);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String cmd = event.getMessage().toLowerCase().split(" ")[0];

        if (isInCombat(player) && blockedCommands.contains(cmd)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Nie możesz używać tej komendy podczas walki!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
    
        if (isInCombat(player)) {
            player.setHealth(0);
            getLogger().info(player.getName() + " wylogował się podczas walki i został zabity.");
        }
    
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    
        combatTagged.remove(uuid);
    }    

    private boolean isInCombat(Player player) {
        if (!combatTagged.containsKey(player.getUniqueId())) return false;
        long timeSinceTagged = System.currentTimeMillis() - combatTagged.get(player.getUniqueId());
        return timeSinceTagged <= COMBAT_DURATION;
    }
}
