package me.mascotasx;

import io.papermc.paper.world.WeatheringCopperState;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.Material;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Cosmeticos visuales que dependen del tipo de mob (colores, variantes). */
public final class Cosmetics {
    private Cosmetics() {}

    private static <T extends Keyed> List<T> list(Registry<T> r) {
        List<T> l = new ArrayList<>();
        for (T t : r) l.add(t);
        return l;
    }

    public static boolean hasColor(Class<?> c) {
        return Sheep.class.isAssignableFrom(c) || Wolf.class.isAssignableFrom(c) || Cat.class.isAssignableFrom(c);
    }

    public static boolean isAgeable(Class<?> c) {
        return Ageable.class.isAssignableFrom(c);
    }

    public static int variantCount(Class<?> c) {
        if (Axolotl.class.isAssignableFrom(c)) return Axolotl.Variant.values().length;
        if (Rabbit.class.isAssignableFrom(c)) return Rabbit.Type.values().length;
        if (Fox.class.isAssignableFrom(c)) return Fox.Type.values().length;
        if (Parrot.class.isAssignableFrom(c)) return Parrot.Variant.values().length;
        if (Horse.class.isAssignableFrom(c)) return Horse.Color.values().length;
        if (Llama.class.isAssignableFrom(c)) return Llama.Color.values().length;
        if (MushroomCow.class.isAssignableFrom(c)) return MushroomCow.Variant.values().length;
        if (Cat.class.isAssignableFrom(c)) return list(Registry.CAT_VARIANT).size();
        if (Wolf.class.isAssignableFrom(c)) return list(Registry.WOLF_VARIANT).size();
        if (Frog.class.isAssignableFrom(c)) return list(Registry.FROG_VARIANT).size();
        if (Villager.class.isAssignableFrom(c)) return list(Registry.VILLAGER_TYPE).size();
        String rf = reflField(c);
        if (rf != null) return reflVariants(rf).size();
        return 0;
    }

    // Variantes de vaca, cerdo y gallina (1.21.5+): se leen por reflexion para no romper en servidores sin ellas.
    private static String reflField(Class<?> c) {
        if (Cow.class.isAssignableFrom(c)) return "COW_VARIANT";
        if (Pig.class.isAssignableFrom(c)) return "PIG_VARIANT";
        if (Chicken.class.isAssignableFrom(c)) return "CHICKEN_VARIANT";
        return null;
    }

    private static List<Object> reflVariants(String field) {
        List<Object> l = new ArrayList<>();
        try {
            Object r = Registry.class.getField(field).get(null);
            for (Object o : (Iterable<?>) r) l.add(o);
        } catch (Throwable ignored) { }
        return l;
    }

    private static void setReflVariant(Object e, String field, int i) {
        List<Object> l = reflVariants(field);
        if (l.isEmpty()) return;
        Object v = l.get(Math.floorMod(i, l.size()));
        for (java.lang.reflect.Method m : e.getClass().getMethods()) {
            if (m.getName().equals("setVariant") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(v)) {
                try { m.invoke(e, v); } catch (Throwable ignored) { }
                return;
            }
        }
    }

    public static void apply(LivingEntity e, PetData d) {
        DyeColor dye = DyeColor.values()[Math.floorMod(d.color, DyeColor.values().length)];
        if (e instanceof Sheep s) s.setColor(dye);
        if (e instanceof Wolf w) { w.setTamed(true); w.setCollarColor(dye); }
        if (e instanceof Cat c) { c.setTamed(true); c.setCollarColor(dye); }

        int n = variantCount(e.getClass());
        if (n == 0) return;
        int i = Math.floorMod(d.variant, n);
        if (e instanceof Axolotl a) a.setVariant(Axolotl.Variant.values()[i]);
        else if (e instanceof Rabbit r) r.setRabbitType(Rabbit.Type.values()[i]);
        else if (e instanceof Fox f) f.setFoxType(Fox.Type.values()[i]);
        else if (e instanceof Parrot p) p.setVariant(Parrot.Variant.values()[i]);
        else if (e instanceof Horse h) h.setColor(Horse.Color.values()[i]);
        else if (e instanceof Llama l) l.setColor(Llama.Color.values()[i]);
        else if (e instanceof MushroomCow m) m.setVariant(MushroomCow.Variant.values()[i]);
        else if (e instanceof Cat c) c.setCatType(list(Registry.CAT_VARIANT).get(i));
        else if (e instanceof Wolf w) w.setVariant(list(Registry.WOLF_VARIANT).get(i));
        else if (e instanceof Frog fr) fr.setVariant(list(Registry.FROG_VARIANT).get(i));
        else if (e instanceof Villager v) v.setVillagerType(list(Registry.VILLAGER_TYPE).get(i));
        else { String rf = reflField(e.getClass()); if (rf != null) setReflVariant(e, rf, i); }
    }

    // ================================================================== cosmeticos unicos por mob
    // Solo cosas que existen en Minecraft vanilla para ese mob (objetos en mano, armaduras, bloque del enderman, etc.).

    public record Extra(String key, String name, Material icon, List<String> labels) {}

    private static final Set<EntityType> ARMOR = EnumSet.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.SKELETON, EntityType.STRAY,
            EntityType.BOGGED, EntityType.WITHER_SKELETON);
    private static final Set<EntityType> HOLD = EnumSet.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.SKELETON, EntityType.STRAY,
            EntityType.BOGGED, EntityType.WITHER_SKELETON, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
            EntityType.WITCH, EntityType.VEX, EntityType.ALLAY, EntityType.COPPER_GOLEM, EntityType.FOX);

    private static final List<String> HAND_DEFAULT = List.of("WOODEN_SWORD", "STONE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "DIAMOND_SWORD",
            "NETHERITE_SWORD", "IRON_AXE", "DIAMOND_AXE", "IRON_PICKAXE", "DIAMOND_PICKAXE", "IRON_SHOVEL", "TRIDENT", "SHIELD", "TORCH",
            "LANTERN", "SOUL_LANTERN", "APPLE", "GOLDEN_APPLE", "CARROT", "BREAD", "COOKED_BEEF", "DIAMOND", "EMERALD", "GOLD_INGOT",
            "NETHERITE_INGOT", "AMETHYST_SHARD", "TOTEM_OF_UNDYING", "POPPY", "DANDELION", "BOOK", "COMPASS", "CLOCK", "SPYGLASS",
            "FISHING_ROD", "BONE", "STICK", "HEART_OF_THE_SEA", "NAUTILUS_SHELL", "BELL", "LEAD", "ENDER_PEARL", "FIREWORK_ROCKET", "COOKIE");
    private static final List<Material> HEADS = List.of(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
            Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET, Material.CARVED_PUMPKIN,
            Material.ZOMBIE_HEAD, Material.SKELETON_SKULL, Material.WITHER_SKELETON_SKULL, Material.CREEPER_HEAD, Material.PIGLIN_HEAD,
            Material.DRAGON_HEAD);
    private static final String[] ARMOR_SETS = {"LEATHER", "CHAINMAIL", "IRON", "GOLDEN", "DIAMOND", "NETHERITE"};
    private static final int[] SLIME_SIZES = {2, 1, 3, 4};
    private static final int[] PHANTOM_SIZES = {0, 2, 4, 6};

    private static String pretty(Material m) { return m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '); }

    private static List<Material> mats(String path, List<String> def, boolean block) {
        List<String> raw = MascotasX.get().getConfig().getStringList(path);
        if (raw.isEmpty()) raw = def;
        List<Material> out = new ArrayList<>();
        for (String s : raw) {
            Material m = Material.matchMaterial(s);
            if (m != null && (block ? m.isBlock() : m.isItem()) && !out.contains(m)) out.add(m);
        }
        return out;
    }
    private static List<Material> handItems() { return mats("cosmetics.hand-items", HAND_DEFAULT, false); }
    private static List<Villager.Type> biomes() { // llanura primero (el aspecto normal)
        List<Villager.Type> l = new ArrayList<>(list(Registry.VILLAGER_TYPE));
        l.remove(Villager.Type.PLAINS);
        l.add(0, Villager.Type.PLAINS);
        return l;
    }

    public static boolean canHold(PetType t) { return HOLD.contains(t.entity()); }
    public static boolean canWear(PetType t) { return ARMOR.contains(t.entity()) || t.entity() == EntityType.COPPER_GOLEM; }
    private static final Set<EntityType> MOUNTS = EnumSet.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.CAMEL);

    /** Ranuras de equipo libres de esta mascota, en el orden en que salen en el menu. */
    public static List<String> slots(PetType t) {
        EntityType et = t.entity();
        List<String> l = new ArrayList<>();
        if (HOLD.contains(et)) { l.add("hand"); l.add("offhand"); }
        if (canWear(t)) l.add("head");
        if (ARMOR.contains(et)) { l.add("chest"); l.add("legs"); l.add("feet"); }
        if (et == EntityType.WOLF || et == EntityType.LLAMA || et == EntityType.TRADER_LLAMA || MOUNTS.contains(et)) l.add("body");
        if (MOUNTS.contains(et) || et == EntityType.PIG) l.add("saddle");
        return l;
    }

    private static EquipmentSlot eqSlot(String key) {
        try {
            return switch (key) {
                case "hand" -> EquipmentSlot.HAND;
                case "offhand" -> EquipmentSlot.OFF_HAND;
                case "head" -> EquipmentSlot.HEAD;
                case "chest" -> EquipmentSlot.CHEST;
                case "legs" -> EquipmentSlot.LEGS;
                case "feet" -> EquipmentSlot.FEET;
                case "body" -> EquipmentSlot.BODY;
                case "saddle" -> EquipmentSlot.valueOf("SADDLE");
                default -> null;
            };
        } catch (Throwable ex) { return null; }
    }

    /** Pone los objetos libres del jugador (cualquier item) encima de los cosmeticos de la lista. */
    public static void applySlots(LivingEntity e, PetData d, PetType t) {
        EntityEquipment eq = e.getEquipment();
        if (eq == null) return;
        for (String key : slots(t)) {
            try {
                ItemStack it = d.custom.get(key);
                EquipmentSlot sl = eqSlot(key);
                if (sl == null) continue;
                boolean preset = key.equals("hand") || key.equals("head") || key.equals("chest") || key.equals("legs")
                        || key.equals("feet") || (key.equals("body") && (e instanceof Wolf || e instanceof Llama));
                if (it == null && preset) continue; // lo gestiona la lista de cosmeticos
                if (key.equals("body") && e instanceof Llama ll) { ll.getInventory().setDecor(it == null ? null : it.clone()); continue; }
                eq.setItem(sl, it == null ? new ItemStack(Material.AIR) : it.clone());
                eq.setDropChance(sl, 0f);
            } catch (Throwable ex) {
                MascotasX.get().getLogger().fine("Ranura " + key + " no aplicable a " + t.id() + ": " + ex.getMessage());
            }
        }
    }

    /** Copia solo visual de un item del jugador (1 unidad). */
    public static ItemStack asCosmetic(ItemStack it) {
        // shulkers, cofres, spawners y bundles se copian sin su contenido (paquetes enormes y datos ajenos a lo visual)
        var meta = it.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.BlockStateMeta || meta instanceof org.bukkit.inventory.meta.BundleMeta)
            return new ItemStack(it.getType());
        ItemStack c = it.clone();
        c.setAmount(1);
        return c;
    }

    private static List<Villager.Profession> professions() {
        List<Villager.Profession> l = new ArrayList<>();
        for (Villager.Profession p : list(Registry.VILLAGER_PROFESSION)) if (p != Villager.Profession.NONE) l.add(p);
        return l;
    }

    /** Arma de serie de cada mob cuando no se elige objeto (lo que llevaria en vanilla). */
    private static Material defaultHand(EntityType et) {
        return switch (et) {
            case VINDICATOR -> Material.IRON_AXE;
            case PIGLIN, ZOMBIFIED_PIGLIN -> Material.GOLDEN_SWORD;
            case PIGLIN_BRUTE -> Material.GOLDEN_AXE;
            case WITHER_SKELETON -> Material.STONE_SWORD;
            case VEX -> Material.IRON_SWORD;
            default -> null;
        };
    }

    private static List<String> names(List<Material> l, String first) {
        List<String> lb = new ArrayList<>(List.of(first));
        for (Material m : l) lb.add(pretty(m));
        return lb;
    }

    /** Cosmeticos unicos disponibles para esta mascota (vacio si el mob no tiene ninguno). */
    public static List<Extra> extras(PetType t) {
        List<Extra> l = new ArrayList<>();
        EntityType et = t.entity();
        if (HOLD.contains(et)) {
            boolean carrier = et == EntityType.ALLAY || et == EntityType.COPPER_GOLEM;
            l.add(new Extra("hand", carrier ? "Objeto que sostiene" : "Objeto en mano", Material.ITEM_FRAME, names(handItems(), "Normal")));
        }
        if (ARMOR.contains(et) || et == EntityType.COPPER_GOLEM)
            l.add(new Extra("head", et == EntityType.COPPER_GOLEM ? "Sombrero" : "Casco / cabeza", Material.IRON_HELMET, names(HEADS, "Ninguno")));
        if (ARMOR.contains(et))
            l.add(new Extra("armor", "Armadura", Material.IRON_CHESTPLATE,
                    List.of("Ninguna", "Cuero", "Malla", "Hierro", "Oro", "Diamante", "Netherite")));
        switch (et) {
            case WOLF -> l.add(new Extra("body", "Armadura de lobo", Material.WOLF_ARMOR, List.of("Ninguna", "Armadura de lobo")));
            case LLAMA, TRADER_LLAMA -> {
                List<String> lb = new ArrayList<>(List.of("Ninguna"));
                for (DyeColor c : DyeColor.values()) lb.add("alfombra " + c.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
                l.add(new Extra("body", "Alfombra", Material.WHITE_CARPET, lb));
            }
            case COPPER_GOLEM -> l.add(new Extra("weather", "Oxidación", Material.COPPER_BLOCK,
                    List.of("Nuevo", "Expuesto", "Desgastado", "Oxidado")));
            case SLIME, MAGMA_CUBE -> l.add(new Extra("size", "Tamaño", Material.SLIME_BALL,
                    List.of("Mediano (2)", "Pequeño (1)", "Grande (3)", "Enorme (4)")));
            case PHANTOM -> l.add(new Extra("size", "Tamaño", Material.PHANTOM_MEMBRANE, List.of("Normal", "Mediano", "Grande", "Enorme")));
            case ZOMBIE_VILLAGER -> {
                List<String> pr = new ArrayList<>(List.of("Ninguna"));
                for (Villager.Profession p : professions()) pr.add(p.getKey().getKey().replace('_', ' '));
                l.add(new Extra("profession", "Profesión", Material.EMERALD, pr));
                List<String> ty = new ArrayList<>();
                for (Villager.Type v : biomes()) ty.add(v.getKey().getKey().replace('_', ' '));
                l.add(new Extra("biome", "Bioma", Material.GRASS_BLOCK, ty));
            }
            case BEE -> l.add(new Extra("nectar", "Néctar", Material.HONEYCOMB, List.of("Sin néctar", "Con néctar")));
            case GOAT -> {
                l.add(new Extra("scream", "Voz", Material.GOAT_HORN, List.of("Normal", "Gritona")));
                l.add(new Extra("horns", "Cuernos", Material.BONE, List.of("Ambos", "Sin cuernos", "Solo izquierdo", "Solo derecho")));
            }
            case PANDA -> {
                List<String> lb = new ArrayList<>();
                for (Panda.Gene g : Panda.Gene.values()) lb.add(g.name().toLowerCase(java.util.Locale.ROOT));
                l.add(new Extra("gene", "Personalidad", Material.BAMBOO, lb));
            }
            case SNOW_GOLEM -> l.add(new Extra("pumpkin", "Calabaza", Material.CARVED_PUMPKIN, List.of("Con calabaza", "Sin calabaza")));
            case VEX -> l.add(new Extra("charge", "Modo ataque", Material.IRON_SWORD, List.of("Normal", "Cargando (ojos rojos)")));
            case ALLAY -> l.add(new Extra("dance", "Baile", Material.JUKEBOX, List.of("Quieto", "Bailando")));
            case SHEEP -> l.add(new Extra("sheared", "Lana", Material.SHEARS, List.of("Con lana", "Esquilada")));
            case HORSE -> {
                List<String> lb = new ArrayList<>();
                for (Horse.Style st : Horse.Style.values()) lb.add(st.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
                l.add(new Extra("style", "Manchas", Material.WHITE_DYE, lb));
            }
            default -> { }
        }
        // ---- estados visuales propios de cada mob (poses, animo, brazos, cofres...)
        if (ARMS.contains(et)) l.add(new Extra("arms", "Brazos", Material.ROTTEN_FLESH, List.of("Normales", "En alto")));
        if (RAIDERS.contains(et)) l.add(new Extra("celebrate", "Celebración", Material.FIREWORK_ROCKET, List.of("No", "Celebrando")));
        switch (et) {
            case WOLF -> {
                l.add(new Extra("mood", "Ánimo", Material.RED_DYE, List.of("Tranquilo", "Enfadado (ojos rojos)", "Curioso (cabeza ladeada)")));
                l.add(new Extra("posture", "Postura", Material.OAK_STAIRS, List.of("De pie", "Se sienta al descansar", "Siempre sentado")));
            }
            case FOX -> {
                l.add(new Extra("action", "Acción", Material.SWEET_BERRIES, List.of("Ninguna", "Agachado", "Cara al suelo", "Curioso")));
                l.add(new Extra("posture", "Postura", Material.OAK_STAIRS,
                    List.of("De pie", "Se sienta al descansar", "Siempre sentado", "Siempre dormido")));
            }
            case PANDA -> {
                l.add(new Extra("posture", "Postura", Material.OAK_STAIRS,
                    List.of("De pie", "Se sienta al descansar", "Siempre sentado", "Rodando")));
                l.add(new Extra("action", "Acción", Material.BAMBOO, List.of("Ninguna", "Estornudando", "Comiendo", "Boca arriba")));
            }
            case POLAR_BEAR -> l.add(new Extra("stand", "Postura", Material.SNOWBALL, List.of("Normal", "De pie sobre las patas")));
            case BEE -> {
                l.add(new Extra("mood", "Ánimo", Material.RED_DYE, List.of("Tranquila", "Enfadada")));
                l.add(new Extra("stung", "Aguijón", Material.HONEYCOMB, List.of("Con aguijón", "Sin aguijón")));
            }
            case CAT -> l.add(new Extra("posture", "Postura", Material.OAK_STAIRS,
                    List.of("De pie", "Se sienta al descansar", "Siempre sentado", "Tumbado")));
            case PARROT, CAMEL -> l.add(new Extra("posture", "Postura", Material.OAK_STAIRS,
                    List.of("De pie", "Se sienta al descansar", "Siempre sentado")));
            case AXOLOTL -> l.add(new Extra("dead", "Pose", Material.AXOLOTL_BUCKET, List.of("Normal", "Haciéndose el muerto")));
            case LLAMA, TRADER_LLAMA -> l.add(new Extra("chest", "Cofres", Material.CHEST, List.of("Sin cofres", "Con cofres")));
            default -> { }
        }
        return l;
    }

    // ================================================================== estetica general (tamano, resplandor, sonidos, efectos)
    private static final Set<EntityType> ARMS = EnumSet.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN);
    private static final Set<EntityType> RAIDERS = EnumSet.of(EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.WITCH);
    private static final Set<UUID> ARMS_UP = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** true si esa mascota debe mantener los brazos en alto (zombis) aunque no pelee. */
    public static boolean armsUp(LivingEntity e) { return ARMS_UP.contains(e.getUniqueId()); }

    private static boolean cfgBool(String path, boolean def) { return MascotasX.get().getConfig().getBoolean(path, def); }

    // ---- tamano
    public static boolean scaleEnabled() { return cfgBool("cosmetics.scale-enabled", true); }
    public static int scaleLevel() { return MascotasX.get().getConfig().getInt("cosmetics.scale-level", 1); }

    public static List<Double> scaleOptions() {
        List<Double> out = new ArrayList<>();
        for (Object o : MascotasX.get().getConfig().getList("cosmetics.scale-options", List.of(0.5, 0.75, 1.0, 1.25, 1.5, 2.0))) {
            try {
                double v = Double.parseDouble(String.valueOf(o));
                if (v >= 0.25 && v <= 4 && !out.contains(v)) out.add(v);
            } catch (NumberFormatException ignored) { }
        }
        if (out.isEmpty()) out.add(1.0);
        return out;
    }

    /** Siguiente (dir=1) o anterior (dir=-1) tamano de la lista, partiendo del mas cercano al actual. */
    public static double stepScale(double cur, int dir) {
        List<Double> o = scaleOptions();
        int best = 0;
        for (int i = 1; i < o.size(); i++) if (Math.abs(o.get(i) - cur) < Math.abs(o.get(best) - cur)) best = i;
        return o.get(Math.floorMod(best + dir, o.size()));
    }

    public static String scaleText(double v) {
        return String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static void applyScale(LivingEntity e, PetData d, double base) {
        AttributeInstance a = e.getAttribute(Attribute.SCALE);
        if (a == null) return;
        a.setBaseValue(Math.max(0.0625, Math.min(16, base * (scaleEnabled() ? d.scale : 1.0))));
    }

    // ---- resplandor (contorno de color con equipos del tablero principal)
    public static final int GLOW_COUNT = 16;
    private static final String TEAM = "mxg_";
    private static final NamedTextColor[] GLOW = {NamedTextColor.WHITE, NamedTextColor.GRAY, NamedTextColor.DARK_GRAY, NamedTextColor.BLACK,
            NamedTextColor.RED, NamedTextColor.DARK_RED, NamedTextColor.GOLD, NamedTextColor.YELLOW, NamedTextColor.GREEN,
            NamedTextColor.DARK_GREEN, NamedTextColor.AQUA, NamedTextColor.DARK_AQUA, NamedTextColor.BLUE, NamedTextColor.DARK_BLUE,
            NamedTextColor.LIGHT_PURPLE, NamedTextColor.DARK_PURPLE};
    public static final List<String> GLOW_LABELS = List.of("Apagado", "<white>Blanco", "<gray>Gris", "<dark_gray>Gris oscuro", "<dark_gray>Negro",
            "<red>Rojo", "<dark_red>Rojo oscuro", "<gold>Dorado", "<yellow>Amarillo", "<green>Verde", "<dark_green>Verde oscuro",
            "<aqua>Aqua", "<dark_aqua>Cian", "<blue>Azul", "<dark_blue>Azul oscuro", "<light_purple>Rosa", "<dark_purple>Morado");

    public static void applyGlow(LivingEntity e, PetData d) {
        var sm = Bukkit.getScoreboardManager();
        if (sm == null) { e.setGlowing(d.glow > 0); return; }
        Scoreboard sb = sm.getMainScoreboard();
        for (int i = 1; i <= GLOW_COUNT; i++) { Team t = sb.getTeam(TEAM + i); if (t != null) t.removeEntity(e); }
        int idx = Math.floorMod(d.glow, GLOW_COUNT + 1);
        if (idx == 0) { e.setGlowing(false); return; }
        Team t = sb.getTeam(TEAM + idx);
        if (t == null) { t = sb.registerNewTeam(TEAM + idx); t.color(GLOW[idx - 1]); }
        t.addEntity(e);
        e.setGlowing(true);
    }

    /** Quita a la mascota de nuestros equipos y libera su estado (al guardarla o eliminarla). */
    public static void forget(LivingEntity e) {
        ARMS_UP.remove(e.getUniqueId());
        var sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard sb = sm.getMainScoreboard();
        for (int i = 1; i <= GLOW_COUNT; i++) { Team t = sb.getTeam(TEAM + i); if (t != null) t.removeEntity(e); }
    }

    /** Al iniciar: vacia los equipos de resplandor (por si el servidor se cerro con mascotas invocadas). */
    public static void resetGlowTeams() {
        var sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard sb = sm.getMainScoreboard();
        for (int i = 1; i <= GLOW_COUNT; i++) {
            Team t = sb.getTeam(TEAM + i);
            if (t != null) for (String en : new ArrayList<>(t.getEntries())) t.removeEntry(en);
        }
    }

    /** Tamano, resplandor y todo lo que no depende del tipo de mob. */
    public static void applyLook(LivingEntity e, PetData d, PetType t) {
        try { applyScale(e, d, MascotasX.get().manager().baseScale(t)); } catch (Throwable ex) { MascotasX.get().getLogger().fine("Escala no aplicable: " + ex.getMessage()); }
        try { applyGlow(e, d); } catch (Throwable ex) { MascotasX.get().getLogger().fine("Resplandor no aplicable: " + ex.getMessage()); }
    }

    // ---- posturas
    private static void flag(Object o, String method, boolean v) { // API que no todos los mobs/versiones tienen: se prueba por nombre
        try { o.getClass().getMethod(method, boolean.class).invoke(o, v); } catch (Throwable ignored) { }
    }

    public static void setSit(LivingEntity e, boolean sit) {
        if (e instanceof Sittable s) s.setSitting(sit); else flag(e, "setSitting", sit);
    }

    // ---- sonidos y efectos
    private static String soundKey(EntityType et) {
        String k = et.getKey().getKey();
        return switch (et) {
            case SLIME, MAGMA_CUBE -> "entity." + k + ".squish";
            case ALLAY -> "entity.allay.ambient_without_item";
            case AXOLOTL -> "entity.axolotl.idle_air";
            case BEE -> "entity.bee.loop";
            case BREEZE -> "entity.breeze.idle_ground";
            case CAVE_SPIDER -> "entity.spider.ambient";
            case TRADER_LLAMA -> "entity.llama.ambient";
            case COPPER_GOLEM -> "block.copper.hit";
            default -> "entity." + k + ".ambient";
        };
    }

    /** Sonido propio de la mascota (o el indicado en pets.yml), mas grave si es grande y mas agudo si es pequena. */
    public static void ambient(LivingEntity e, String override, float vol) {
        String key = override != null && !override.isBlank() ? override : soundKey(e.getType());
        double sc = 1;
        AttributeInstance a = e.getAttribute(Attribute.SCALE);
        if (a != null) sc = a.getValue();
        float pitch = (float) Math.max(0.5, Math.min(2.0, 1.0 / Math.sqrt(Math.max(0.25, sc))));
        e.getWorld().playSound(e.getLocation(), key, vol, pitch);
    }

    private static Location mid(LivingEntity e) { return e.getLocation().add(0, e.getHeight() / 2, 0); }

    public static void fxSummon(LivingEntity e, PetData d, String sound) {
        if (!cfgBool("effects.summon", true)) return;
        World w = e.getWorld();
        w.spawnParticle(Particle.CLOUD, mid(e), 14, 0.3, 0.3, 0.3, 0.03);
        w.spawnParticle(Particle.END_ROD, mid(e), 8, 0.4, 0.4, 0.4, 0.02);
        if (!d.muted) ambient(e, sound, 0.8f);
    }

    public static void fxStore(LivingEntity e) {
        if (!cfgBool("effects.store", true)) return;
        e.getWorld().spawnParticle(Particle.POOF, mid(e), 12, 0.3, 0.3, 0.3, 0.04);
    }

    public static void fxLevel(LivingEntity e) {
        if (!cfgBool("effects.level-up", true)) return;
        World w = e.getWorld();
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, mid(e), 25, 0.4, 0.5, 0.4, 0.3);
        w.spawnParticle(Particle.HAPPY_VILLAGER, mid(e), 10, 0.5, 0.5, 0.5, 0.0);
    }

    public static void applyExtras(LivingEntity e, PetData d, PetType t) {
        if (e instanceof CopperGolem cg) { // encerado: si se oxida del todo se convierte en estatua y la mascota desapareceria
            try { cg.setOxidizing(CopperGolem.Oxidizing.waxed()); } catch (Throwable ignored) { }
        }
        for (Extra x : extras(t)) {
            int idx = Math.floorMod(d.extra.getOrDefault(x.key(), 0), x.labels().size());
            try {
                applyOne(e, d, t, x.key(), idx);
            } catch (Throwable ex) {
                MascotasX.get().getLogger().fine("Cosmetico " + x.key() + " no aplicable a " + t.id() + ": " + ex.getMessage());
            }
        }
        applySlots(e, d, t);
    }

    private static void applyOne(LivingEntity e, PetData d, PetType t, String key, int idx) {
        EntityEquipment eq = e.getEquipment();
        switch (key) {
            case "hand" -> {
                if (eq == null) return;
                ItemStack cu = d.custom.get("hand"); // objeto libre elegido por el jugador
                if (cu != null) eq.setItemInMainHand(cu.clone());
                else if (idx == 0) {
                    Material m = defaultHand(t.entity());
                    if ("arrow".equals(t.attack())) m = t.entity() == EntityType.PILLAGER ? Material.CROSSBOW : Material.BOW;
                    eq.setItemInMainHand(m == null ? new ItemStack(Material.AIR) : new ItemStack(m));
                } else eq.setItemInMainHand(new ItemStack(handItems().get(idx - 1)));
                eq.setItemInMainHandDropChance(0f);
            }
            case "head" -> {
                if (eq == null) return;
                ItemStack cu = d.custom.get("head");
                if (cu != null) eq.setHelmet(cu.clone());
                else eq.setHelmet(idx == 0 ? new ItemStack(Material.AIR) : new ItemStack(HEADS.get(idx - 1)));
                eq.setHelmetDropChance(0f);
            }
            case "armor" -> {
                if (eq == null) return;
                String set = idx == 0 ? null : ARMOR_SETS[idx - 1];
                eq.setChestplate(set == null ? new ItemStack(Material.AIR) : new ItemStack(Material.valueOf(set + "_CHESTPLATE")));
                eq.setLeggings(set == null ? new ItemStack(Material.AIR) : new ItemStack(Material.valueOf(set + "_LEGGINGS")));
                eq.setBoots(set == null ? new ItemStack(Material.AIR) : new ItemStack(Material.valueOf(set + "_BOOTS")));
                eq.setChestplateDropChance(0f);
                eq.setLeggingsDropChance(0f);
                eq.setBootsDropChance(0f);
            }
            case "body" -> {
                if (e instanceof Llama ll) {
                    ll.getInventory().setDecor(idx == 0 ? null : new ItemStack(Material.valueOf(DyeColor.values()[idx - 1].name() + "_CARPET")));
                } else if (eq != null) {
                    eq.setItem(EquipmentSlot.BODY, idx == 0 ? new ItemStack(Material.AIR) : new ItemStack(Material.WOLF_ARMOR));
                    eq.setDropChance(EquipmentSlot.BODY, 0f);
                }
            }
            case "weather" -> {
                if (e instanceof CopperGolem cg) cg.setWeatheringState(WeatheringCopperState.values()[idx]);
            }
            case "size" -> {
                if (e instanceof Slime sl) sl.setSize(SLIME_SIZES[idx]);
                else if (e instanceof Phantom ph) ph.setSize(PHANTOM_SIZES[idx]);
            }
            case "profession" -> {
                if (e instanceof ZombieVillager zv) zv.setVillagerProfession(idx == 0 ? Villager.Profession.NONE : professions().get(idx - 1));
            }
            case "biome" -> {
                if (e instanceof ZombieVillager zv) zv.setVillagerType(biomes().get(idx));
            }
            case "nectar" -> { if (e instanceof Bee b) b.setHasNectar(idx == 1); }
            case "scream" -> { if (e instanceof Goat g) g.setScreaming(idx == 1); }
            case "horns" -> {
                if (e instanceof Goat g) { g.setLeftHorn(idx == 0 || idx == 2); g.setRightHorn(idx == 0 || idx == 3); }
            }
            case "gene" -> {
                if (e instanceof Panda p) { Panda.Gene g = Panda.Gene.values()[idx]; p.setMainGene(g); p.setHiddenGene(g); }
            }
            case "pumpkin" -> { if (e instanceof Snowman sn) sn.setDerp(idx == 1); }
            case "charge" -> { if (e instanceof Vex v) v.setCharging(idx == 1); }
            case "dance" -> {
                if (e instanceof Allay a) { if (idx == 1) a.startDancing(); else a.stopDancing(); }
            }
            case "arms" -> {
                if (idx == 1) ARMS_UP.add(e.getUniqueId()); else ARMS_UP.remove(e.getUniqueId());
                if (e instanceof Mob m) m.setAggressive(idx == 1);
            }
            case "celebrate" -> { if (e instanceof Raider r) r.setCelebrating(idx == 1); }
            case "mood" -> {
                if (e instanceof Wolf w) { flag(w, "setAngry", idx == 1); flag(w, "setInterested", idx == 2); }
                else if (e instanceof Bee b) b.setAnger(idx == 1 ? 1_000_000 : 0);
            }
            case "posture" -> {
                if (idx == 1) return; // "se sienta al descansar": lo controla PetManager segun lo que hace la mascota
                setSit(e, idx == 2);
                if (e instanceof Fox) flag(e, "setSleeping", idx == 3);
                if (e instanceof Panda) flag(e, "setRolling", idx == 3);
                if (e instanceof Cat) flag(e, "setLyingDown", idx == 3);
            }
            case "sheared" -> { if (e instanceof Sheep sh) sh.setSheared(idx == 1); }
            case "style" -> { if (e instanceof Horse h) h.setStyle(Horse.Style.values()[idx]); }
            case "stung" -> { if (e instanceof Bee b) b.setHasStung(idx == 1); }
            case "action" -> {
                if (e instanceof Fox) { flag(e, "setCrouching", idx == 1); flag(e, "setFaceplanted", idx == 2); flag(e, "setInterested", idx == 3); }
                if (e instanceof Panda) { flag(e, "setSneezing", idx == 1); flag(e, "setEating", idx == 2); flag(e, "setOnBack", idx == 3); }
            }
            case "stand" -> flag(e, "setStanding", idx == 1);
            case "dead" -> { if (e instanceof Axolotl a) a.setPlayingDead(idx == 1); }
            case "chest" -> { if (e instanceof ChestedHorse c) c.setCarryingChest(idx == 1); }
            default -> { }
        }
    }
}
