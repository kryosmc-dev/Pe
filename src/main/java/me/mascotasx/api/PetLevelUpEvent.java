package me.mascotasx.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Una mascota sube de nivel (se lanza antes de entregar la recompensa). */
public class PetLevelUpEvent extends PetEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int level;

    public PetLevelUpEvent(Player owner, String petId, int level) {
        super(owner, petId);
        this.level = level;
    }

    /** Nivel que acaba de alcanzar. */
    public int getLevel() { return level; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
