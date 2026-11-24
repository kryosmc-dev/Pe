package me.mascotasx;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PetListener implements Listener {
    private final MascotasX plugin;
    private final PetManager manager;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>(); // el chat llega por un hilo asincrono

    public PetListener(MascotasX plugin) {
        this.plugin = plugin;
        this.manager = plugin.manager();
    }

    public void handleJoin(Player p) {
        Profile pr = manager.profile(p.getUniqueId());
        if (plugin.getConfig().getBoolean("auto-summon", true) && pr.active != null && pr.owned.containsKey(pr.active)
                && plugin.worldAllowed(p.getWorld())) {
            String id = pr.active;
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) manager.summon(p, id); }, 20L);
        }
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent ev) { handleJoin(ev.getPlayer()); }

    /** Al volver de un mundo con mascotas desactivadas (o tras un fallo), la mascota guardada vuelve sola. */
    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent ev) {
        Player p = ev.getPlayer();
        Profile pr = manager.profile(p.getUniqueId());
        if (pr.active == null || !pr.owned.containsKey(pr.active) || manager.hasActive(p) || !plugin.worldAllowed(p.getWorld())) return;
        String id = pr.active;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !manager.hasActive(p) && plugin.worldAllowed(p.getWorld())) manager.summon(p, id, false);
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent ev) {
        pending.remove(ev.getPlayer().getUniqueId());
        manager.unload(ev.getPlayer());
    }

    @EventHandler
    public void onInput(PlayerInputEvent ev) {
        manager.setInput(ev.getPlayer().getUniqueId(), ev.getInput());
    }

    /** Click derecho sobre tu mascota = abre su menu de datos. */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent ev) {
        UUID owner = manager.ownerOf(ev.getRightClicked());
        if (owner == null) return;
        ev.setCancelled(true); // siempre (tambien mano secundaria): sin name tags, leads ni baldes sobre mascotas
        if (ev instanceof PlayerInteractAtEntityEvent) return; // el cliente manda "interact at" + "interact": el menu se abre una sola vez
        if (ev.getHand() != EquipmentSlot.HAND) return;
        Player p = ev.getPlayer();
        if (owner.equals(p.getUniqueId())) {
            Profile pr = manager.profile(p.getUniqueId());
            if (pr.active != null) new DataMenu(plugin, p, pr.active).open();
        }
    }

    /** Agachado + clic derecho sobre un bloque: la mascota que llevas de sombrero se baja de tu cabeza. */
    @EventHandler
    public void onDropHat(org.bukkit.event.player.PlayerInteractEvent ev) {
        if (ev.getHand() != EquipmentSlot.HAND || !ev.getPlayer().isSneaking()
                || ev.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player p = ev.getPlayer();
        LivingEntity pet = manager.activeEntity(p);
        if (pet != null && p.getPassengers().contains(pet)) {
            p.removePassenger(pet);
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent ev) {
        if (manager.isPet(ev.getEntity()) || manager.isPet(ev.getTarget())) ev.setCancelled(true);
    }

    /** Un portal (nether, end, gateway) crearia una copia de la mascota en otra dimension y la dejaria huerfana. */
    @EventHandler(ignoreCancelled = true)
    public void onPortal(EntityPortalEvent ev) {
        if (manager.isPet(ev.getEntity())) ev.setCancelled(true);
    }

    /** Si una mascota slime/cubo de magma muere (vacio, /kill), no debe soltar slimes hostiles. */
    @EventHandler(ignoreCancelled = true)
    public void onSlimeSplit(SlimeSplitEvent ev) {
        if (manager.isPet(ev.getEntity())) ev.setCancelled(true);
    }

    /** Las bolas de fuego de las mascotas no prenden bloques (ni la base del dueno). */
    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent ev) {
        Entity ig = ev.getIgnitingEntity();
        if (ig instanceof Projectile pr && pr.getShooter() instanceof Entity s && manager.isPet(s)) ev.setCancelled(true);
        else if (manager.isPet(ig)) ev.setCancelled(true);
    }

    /** Vault suele registrar su economia DESPUES de que cargue este plugin: se enlaza en cuanto aparece. */
    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent ev) {
        if (plugin.economy() != null) return;
        if (!"net.milkbowl.vault.economy.Economy".equals(ev.getProvider().getService().getName())) return;
        Bukkit.getScheduler().runTask(plugin, () -> plugin.setupEconomy(true));
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent ev) {
        if (manager.isPet(ev.getEntity())) ev.setCancelled(true);
    }

    /** Quien causa realmente el dano: el tirador de un proyectil o el dueno de unos colmillos. */
    private static Entity source(Entity raw) {
        if (raw instanceof Projectile pr && pr.getShooter() instanceof Entity s) return s;
        if (raw instanceof EvokerFangs f && f.getOwner() != null) return f.getOwner();
        return raw;
    }

    /** Las transformaciones vanilla (zombi->ahogado, esqueleto->stray, piglin->zombificado) romperian la mascota. */
    @EventHandler(ignoreCancelled = true)
    public void onTransform(EntityTransformEvent ev) {
        if (manager.isPet(ev.getEntity())) ev.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageBy(EntityDamageByEntityEvent ev) {
        Entity raw = ev.getDamager();
        Entity src = source(raw);
        Entity victim = ev.getEntity();
        if (manager.isPet(victim) || (manager.isPet(src) && !(victim instanceof Enemy))) {
            ev.setCancelled(true);
            return;
        }
        if (raw instanceof Snowball && manager.isPet(src)) { ev.setCancelled(true); return; } // el dano de nieve lo aplica onProjectileHit
        UUID ow = manager.ownerOf(src);
        if (ow != null && !(victim instanceof Player)) manager.markHit(victim, ow); // para dar XP aunque muera por fuego, etc.
        // Proyectiles / colmillos de una mascota: el dano depende de su nivel (nieve y viento se aplican en ProjectileHit)
        if (manager.isPet(src) && src != raw && !(raw instanceof WindCharge))
            ev.setDamage(manager.petDamage(src));
        // Solo se fijan como objetivo enemigos reales (no aldeanos, animales, armor stands ni jugadores)
        if (src instanceof Player pl && victim instanceof Enemy && victim instanceof LivingEntity lv) {
            manager.setTarget(pl, lv, 1);      // tu objetivo
        } else if (victim instanceof Player pl && src instanceof Enemy && src instanceof LivingEntity lv) {
            manager.setTarget(pl, lv, 2);      // quien te ataca pasa primero
        }
    }

    /** La pocion de la bruja es solo visual: no aplica efectos a nadie; el dano se aplica directo a los enemigos. */
    @EventHandler
    public void onSplash(PotionSplashEvent ev) {
        if (!(ev.getPotion().getShooter() instanceof LivingEntity pet) || !manager.isPet(pet)) return;
        double dmg = manager.petDamage(pet);
        for (LivingEntity le : new ArrayList<>(ev.getAffectedEntities())) {
            ev.setIntensity(le, 0); // sin veneno ni efectos
            if (le instanceof Enemy && !(le instanceof Player) && !manager.isPet(le)) le.damage(dmg, pet);
        }
    }

    /** Bolas de nieve y de viento de las mascotas: dano directo, sin empujar ni quemar al dueno. */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent ev) {
        Projectile pr = ev.getEntity();
        if (!(pr.getShooter() instanceof LivingEntity pet) || !manager.isPet(pet)) return;
        if (pr instanceof AbstractArrow && ev.getHitBlock() != null) { pr.remove(); return; } // sin flechas clavadas por el suelo
        if (!(pr instanceof Snowball) && !(pr instanceof WindCharge)) return;
        if (pr instanceof WindCharge) {
            ev.setCancelled(true); // sin explosion: no empuja a nadie
            pr.getWorld().spawnParticle(Particle.CLOUD, pr.getLocation(), 8, 0.2, 0.2, 0.2, 0.05);
            pr.remove();
        }
        if (ev.getHitEntity() instanceof LivingEntity le && le instanceof Enemy && !manager.isPet(le)) {
            le.damage(manager.petDamage(pet), pet);
            org.bukkit.util.Vector pv = pr.getVelocity();
            if (pr instanceof WindCharge && pv.lengthSquared() > 1.0E-6)
                le.setVelocity(le.getVelocity().add(pv.clone().normalize().multiply(0.5).setY(0.25)));
        }
    }

    @EventHandler
    public void onCombust(EntityCombustByEntityEvent ev) {
        Entity src = source(ev.getCombuster());
        if (manager.isPet(src) && (ev.getEntity() instanceof Player || manager.isPet(ev.getEntity()))) ev.setCancelled(true);
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent ev) {
        if (manager.isPet(ev.getEntity())) ev.setCancelled(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent ev) {
        LivingEntity v = ev.getEntity();
        if (manager.isPet(v)) { // una mascota que muere (vacio, /kill, otro plugin) no debe soltar botin ni XP
            ev.getDrops().clear();
            ev.setDroppedExp(0);
            return;
        }
        if (v instanceof Player || !(v instanceof Enemy)) return; // XP solo por enemigos
        Player owner = null;
        Player killer = v.getKiller();
        if (killer != null && manager.hasActive(killer)) {
            owner = killer;
        } else if (v.getLastDamageCause() instanceof EntityDamageByEntityEvent dbe) {
            UUID o = manager.ownerOf(source(dbe.getDamager()));
            if (o != null) owner = Bukkit.getPlayer(o);
        }
        UUID credit = manager.creditFor(v); // ultimo golpe de una mascota (fuego, etc.)
        if (owner == null && credit != null) owner = Bukkit.getPlayer(credit);
        if (owner != null && owner.isOnline()) manager.reward(owner, v);
    }

    // --------------------------------------------------------------- entrada por chat
    /** Cierra el menu, muestra el aviso y ejecuta el handler con el siguiente mensaje del jugador. */
    public void awaitInput(Player p, String prompt, Consumer<String> handler) {
        pending.put(p.getUniqueId(), handler);
        p.closeInventory();
        p.sendMessage(Text.c(prompt));
    }

    public void awaitName(Player p, String petId) {
        pending.put(p.getUniqueId(), text -> {
            int max = plugin.getConfig().getInt("cosmetics.max-name-length", 16);
            String name = text.length() > max ? text.substring(0, max) : text;
            name = name.replace("<", "").replace(">", "");
            if (name.isEmpty()) { plugin.msg(p, "name-cancel"); return; }
            PetData d = manager.profile(p.getUniqueId()).owned.get(petId);
            if (d == null) return;
            d.name = name;
            manager.save(p.getUniqueId());
            manager.refresh(p);
            plugin.msg(p, "name-set", "%name%", name);
            new CosmeticsMenu(plugin, p, petId).open();
        });
        p.closeInventory();
        plugin.msg(p, "name-prompt");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent ev) {
        Player p = ev.getPlayer();
        Consumer<String> h = pending.remove(p.getUniqueId());
        if (h == null) return;
        ev.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(ev.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancelar")) { plugin.msg(p, "name-cancel"); return; }
            h.accept(text);
        });
    }
}
