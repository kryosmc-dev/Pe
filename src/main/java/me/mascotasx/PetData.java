package me.mascotasx;

public class PetData {
    public final String type;
    public int level = 1;
    public double xp = 0;
    public String name;            // nombre personalizado (null = el del tipo)
    public boolean muted, burning, upside, baby;
    public PetMode mode = PetMode.AGGRESSIVE;
    public int color, variant;
    public int glow;               // resplandor: 0 apagado, 1-16 color del contorno
    public double scale = 1.0;     // tamano elegido por el jugador (se multiplica por el base-scale de la mascota)
    public final java.util.Map<String, Integer> extra = new java.util.HashMap<>(); // cosmeticos unicos: clave -> opcion elegida
    public final java.util.Map<String, org.bukkit.inventory.ItemStack> custom = new java.util.HashMap<>(); // objetos libres puestos por el jugador (hand, head)
    public String effect;          // efecto equipado (solo 1 a la vez)

    public PetData(String type) { this.type = type; }

    public String displayName(PetType t) { return name != null ? name : t.name(); }
}
