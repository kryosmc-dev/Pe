package me.mascotasx;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public record PetType(String id, EntityType entity, String name, double price, double damage,
                      Material icon, int page, int slot, boolean flying, String attack, boolean enabled,
                      PetCombat combat, double rewardMult) {
    public boolean ranged() { return !"melee".equals(attack) && !"none".equals(attack); }
    /** true si la mascota no pelea (mobs pasivos y monturas). */
    public boolean peaceful() { return "none".equals(attack); }
}
