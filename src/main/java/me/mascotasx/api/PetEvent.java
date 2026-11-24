package me.mascotasx.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/** Base de los eventos de MascotasX. */
public abstract class PetEvent extends Event {
    private final Player owner;
    private final String petId;

    protected PetEvent(Player owner, String petId) {
        this.owner = owner;
        this.petId = petId;
    }

    /** Dueno de la mascota. */
    public Player getOwner() { return owner; }

    /** Id de la mascota (la clave en pets.yml, por ejemplo "zombie"). */
    public String getPetId() { return petId; }
}
