package me.mascotasx.api;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** La mascota golpea o dispara a un objetivo. Cancelarlo anula ese ataque. El dano es informativo. */
public class PetAttackEvent extends PetEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final LivingEntity pet;
    private final LivingEntity target;
    private final double damage;
    private final boolean ranged;
    private boolean cancelled;

    public PetAttackEvent(Player owner, String petId, LivingEntity pet, LivingEntity target, double damage, boolean ranged) {
        super(owner, petId);
        this.pet = pet;
        this.target = target;
        this.damage = damage;
        this.ranged = ranged;
    }

    public LivingEntity getPet() { return pet; }
    public LivingEntity getTarget() { return target; }
    /** Dano que se aplicara (segun nivel). */
    public double getDamage() { return damage; }
    /** true = disparo a distancia, false = golpe cuerpo a cuerpo. */
    public boolean isRanged() { return ranged; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
