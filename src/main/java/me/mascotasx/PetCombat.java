package me.mascotasx;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Ajustes de combate de una mascota. Todo es opcional en pets.yml (seccion "combat:" de cada mascota);
 * lo que falte usa el valor por defecto de su tipo de ataque.
 *
 * @param windup        ms que "carga" antes de disparar (solo a distancia)
 * @param cooldown      ms minimos entre disparos (solo a distancia)
 * @param meleeCooldown ms minimos entre golpes cuerpo a cuerpo
 * @param range         alcance maximo de disparo, en bloques
 * @param detect        radio alrededor del jugador donde busca enemigos
 * @param keepDistance  distancia que intenta mantener un atacante a distancia (0 = no retrocede)
 * @param speed         velocidad al perseguir
 */
public record PetCombat(long windup, long cooldown, long meleeCooldown, double range, double detect,
                        double keepDistance, double speed) {

    public static PetCombat defaults(String attack) {
        return switch (attack) {
            case "arrow" -> new PetCombat(1000, 1500, 900, 16, 12, 6, 0.34);
            case "fireball" -> new PetCombat(800, 1800, 900, 16, 12, 5, 0.34);
            case "snowball" -> new PetCombat(350, 900, 900, 16, 12, 4, 0.34);
            case "spit" -> new PetCombat(900, 1600, 900, 16, 12, 5, 0.34);
            case "potion" -> new PetCombat(1100, 2200, 900, 16, 12, 6, 0.34);
            case "wind" -> new PetCombat(1000, 2200, 900, 16, 12, 5, 0.34);
            case "fangs" -> new PetCombat(1100, 2800, 900, 10, 12, 5, 0.34);
            case "melee" -> new PetCombat(0, 0, 900, 0, 8, 0, 0.34);
            default -> new PetCombat(800, 1500, 900, 16, 12, 5, 0.34);
        };
    }

    public static PetCombat read(ConfigurationSection pet, String attack) {
        PetCombat d = defaults(attack);
        ConfigurationSection s = pet == null ? null : pet.getConfigurationSection("combat");
        if (s == null) return d;
        double range = clamp(s.getDouble("range", d.range), 0, 48);
        return new PetCombat(
                (long) clamp(s.getLong("windup", d.windup), 0, 10000),
                (long) clamp(s.getLong("cooldown", d.cooldown), 100, 60000),
                (long) clamp(s.getLong("melee-cooldown", d.meleeCooldown), 100, 60000),
                range,
                clamp(s.getDouble("detect", d.detect), 3, 32),
                clamp(s.getDouble("keep-distance", d.keepDistance), 0, Math.max(0, range * 0.8)),
                clamp(s.getDouble("speed", d.speed), 0.1, 0.8));
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
