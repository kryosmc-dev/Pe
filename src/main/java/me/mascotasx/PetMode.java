package me.mascotasx;

import java.util.Locale;

/** Comportamiento de combate de la mascota. */
public enum PetMode {
    PASSIVE,     // nunca ataca
    DEFENSIVE,   // solo ataca a quien te golpea o a quien tu golpeas
    AGGRESSIVE;  // ademas, busca enemigos cercanos por su cuenta

    public PetMode next() { return values()[(ordinal() + 1) % values().length]; }

    /** Acepta espanol e ingles; devuelve def si no se reconoce. */
    public static PetMode parse(String s, PetMode def) {
        if (s == null) return def;
        return switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "passive", "pasivo", "pasiva" -> PASSIVE;
            case "defensive", "defensivo", "defensiva" -> DEFENSIVE;
            case "aggressive", "agresivo", "agresiva" -> AGGRESSIVE;
            default -> def;
        };
    }
}
