package me.mascotasx.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Una mascota va a ganar experiencia. Se puede cambiar la cantidad o cancelar. */
public class PetXpGainEvent extends PetEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private double amount;
    private boolean cancelled;

    public PetXpGainEvent(Player owner, String petId, double amount) {
        super(owner, petId);
        this.amount = amount;
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
