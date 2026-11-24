package me.mascotasx;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Menu de cosmeticos al estilo GPet: las opciones de cada mob salen ordenadas desde la primera casilla util,
 * debajo van las ranuras de equipo libres (cualquier objeto) y la fila inferior tiene los botones fijos.
 */
public class CosmeticsMenu extends Menu {
    private final String petId;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public CosmeticsMenu(MascotasX plugin, Player p, String petId) {
        super(plugin, p, plugin.lang().s("titles.cosmetics"));
        this.petId = petId;
    }

    private int mountLevel() { return plugin.getConfig().getInt("cosmetics.mount-level", 10); }
    private int hatLevel() { return plugin.getConfig().getInt("cosmetics.hat-level", 5); }
    private String state(boolean b) { return plugin.lang().s(b ? "words.state-on" : "words.state-off"); }
    private String status(boolean ok, int req) {
        return plugin.lang().s(ok ? "words.click-use" : "words.need-level", "%level%", String.valueOf(req));
    }

    private static Material slotIcon(String key) {
        return switch (key) {
            case "hand" -> Material.IRON_SWORD;
            case "offhand" -> Material.SHIELD;
            case "head" -> Material.IRON_HELMET;
            case "chest" -> Material.IRON_CHESTPLATE;
            case "legs" -> Material.IRON_LEGGINGS;
            case "feet" -> Material.IRON_BOOTS;
            case "body" -> Material.LEATHER_HORSE_ARMOR;
            default -> Material.SADDLE;
        };
    }

    private LivingEntity activePet() {
        return plugin.manager().isActive(player, petId) ? plugin.manager().activeEntity(player) : null;
    }

    @Override
    protected void build() {
        fill();
        actions.clear();
        PetData d = data(petId);
        PetType t = plugin.manager().type(petId);
        if (d == null || t == null) { abort(); return; }
        Class<?> cls = t.entity().getEntityClass();
        if (cls == null) cls = org.bukkit.entity.Entity.class;
        final Class<?> mob = cls;
        Lang L = plugin.lang();

        inv.setItem(4, L.item(t.icon(), "items.data.info", "%name%", d.displayName(t),
                "%level%", String.valueOf(d.level), "%max%", String.valueOf(plugin.manager().maxLevel()),
                "%xpline%", PetsMenu.xpLine(plugin, d), "%damage%", Text.num(plugin.manager().damage(t, d))));

        // ---- opciones de este mob, en orden
        int n = 0;
        if (Cosmetics.isAgeable(mob)) {
            inv.setItem(Menu.grid(n), Items.glow(L.item(Material.WHEAT, "items.cosmetics.baby", "%state%", state(d.baby)), d.baby));
            actions.put(Menu.grid(n++), e -> { d.baby = !d.baby; changed(); });
        }
        if (Cosmetics.hasColor(mob)) {
            DyeColor dye = DyeColor.values()[Math.floorMod(d.color, 16)];
            Material m = Material.matchMaterial(dye.name() + "_DYE");
            inv.setItem(Menu.grid(n), L.item(m == null ? Material.INK_SAC : m, "items.cosmetics.color", "%current%", dye.name().toLowerCase().replace('_', ' ')));
            actions.put(Menu.grid(n++), e -> { d.color = Math.floorMod(d.color + (e.isRightClick() ? -1 : 1), 16); changed(); });
        }
        int vc = Cosmetics.variantCount(mob);
        if (vc > 0) {
            inv.setItem(Menu.grid(n), L.item(Material.PAINTING, "items.cosmetics.variant", "%index%", String.valueOf(Math.floorMod(d.variant, vc) + 1), "%total%", String.valueOf(vc)));
            actions.put(Menu.grid(n++), e -> { d.variant = Math.floorMod(d.variant + (e.isRightClick() ? -1 : 1), vc); changed(); });
        }
        for (Cosmetics.Extra x : Cosmetics.extras(t)) {
            if (n >= 21) break;
            String base = "extras." + t.entity().name() + "." + x.key();
            List<String> labels = L.autoList(base + ".labels", x.labels());
            int idx = Math.floorMod(d.extra.getOrDefault(x.key(), 0), labels.size());
            ItemStack cu = d.custom.get(x.key());
            String cur = cu != null ? L.s("words.custom", "%item%", cu.getType().name().toLowerCase().replace('_', ' ')) : labels.get(idx);
            inv.setItem(Menu.grid(n), Items.glow(L.item(x.icon(), "items.cosmetics.extra", "%name%", L.auto(base + ".name", x.name()),
                    "%current%", cur, "%index%", String.valueOf(idx + 1), "%total%", String.valueOf(labels.size())), idx > 0 || cu != null));
            final int size = x.labels().size();
            actions.put(Menu.grid(n++), e -> {
                int c = Math.floorMod(d.extra.getOrDefault(x.key(), 0), size);
                d.extra.put(x.key(), e.isShiftClick() ? 0 : e.isRightClick() ? Math.floorMod(c - 1, size) : Math.floorMod(c + 1, size));
                d.custom.remove(x.key()); // al usar la lista se quita el objeto libre
                changed();
            });
        }
        List<String> glowNames = L.autoList("glow.labels", Cosmetics.GLOW_LABELS);
        int gi = Math.floorMod(d.glow, Cosmetics.GLOW_COUNT + 1);
        inv.setItem(Menu.grid(n), Items.glow(L.item(Material.GLOWSTONE_DUST, "items.cosmetics.glow", "%current%", glowNames.get(Math.min(gi, glowNames.size() - 1))), gi > 0));
        actions.put(Menu.grid(n++), e -> {
            int c = Cosmetics.GLOW_COUNT + 1;
            d.glow = e.isShiftClick() ? 0 : e.isRightClick() ? Math.floorMod(d.glow - 1, c) : Math.floorMod(d.glow + 1, c);
            changed();
        });

        // ---- ranuras de equipo libres (siguiente fila)
        int pos = ((n + 6) / 7) * 7;
        for (String key : Cosmetics.slots(t)) {
            if (pos >= 28) break;
            ItemStack cu = d.custom.get(key);
            String cur = cu == null ? L.s("words.object-none") : cu.getType().name().toLowerCase().replace('_', ' ');
            ItemStack it = L.item(cu == null ? slotIcon(key) : cu.getType(), "items.cosmetics.slot", "%name%", L.s("words.slot-" + key), "%current%", cur);
            inv.setItem(Menu.grid(pos), cu == null ? it : Items.glow(it, true));
            actions.put(Menu.grid(pos++), e -> setSlot(d, key, e));
        }

        // ---- fila inferior fija
        inv.setItem(45, L.item(Material.COOKIE, "items.cosmetics.pet"));
        actions.put(45, e -> {
            LivingEntity pet = activePet();
            if (pet == null) { plugin.msg(player, "need-active"); return; }
            pet.getWorld().spawnParticle(Particle.HEART, pet.getLocation().add(0, pet.getHeight() + 0.3, 0), 6, 0.4, 0.3, 0.4);
            if (!d.muted) Cosmetics.ambient(pet, plugin.manager().petSound(t), 1f);
            plugin.msg(player, "petted");
        });
        inv.setItem(46, L.item(Material.SADDLE, "items.cosmetics.mount", "%status%", status(d.level >= mountLevel(), mountLevel())));
        actions.put(46, e -> ride(d, true));
        inv.setItem(47, L.item(Material.DIAMOND_HELMET, "items.cosmetics.hat", "%status%", status(d.level >= hatLevel(), hatLevel())));
        actions.put(47, e -> ride(d, false));
        inv.setItem(48, L.item(Material.NAME_TAG, "items.cosmetics.name", "%name%", d.displayName(t)));
        actions.put(48, e -> plugin.listener().awaitName(player, petId));
        inv.setItem(49, Items.glow(L.item(Material.CHORUS_FLOWER, "items.cosmetics.flip", "%state%", state(d.upside)), d.upside));
        actions.put(49, e -> { d.upside = !d.upside; changed(); });
        if (Cosmetics.scaleEnabled()) {
            inv.setItem(50, L.item(Material.SPYGLASS, "items.cosmetics.scale", "%current%", Cosmetics.scaleText(d.scale),
                    "%status%", status(d.level >= Cosmetics.scaleLevel(), Cosmetics.scaleLevel())));
            actions.put(50, e -> {
                if (d.level < Cosmetics.scaleLevel()) { plugin.msg(player, "locked", "%level%", String.valueOf(Cosmetics.scaleLevel())); return; }
                d.scale = e.isShiftClick() ? 1.0 : Cosmetics.stepScale(d.scale, e.isRightClick() ? -1 : 1);
                changed();
            });
        }
        L.flush();
        inv.setItem(52, L.item(Material.BARRIER, "items.common.back"));
        actions.put(52, e -> new DataMenu(plugin, player, petId).open());
    }

    private void ride(PetData d, boolean mount) {
        int req = mount ? mountLevel() : hatLevel();
        if (d.level < req) { plugin.msg(player, "locked", "%level%", String.valueOf(req)); return; }
        LivingEntity pet = activePet();
        if (pet == null) { plugin.msg(player, "need-active"); return; }
        if (mount) {
            if (player.getVehicle() == pet) player.leaveVehicle();
            else { player.removePassenger(pet); pet.addPassenger(player); }
        } else {
            if (pet.getHeight() > 1.6) { plugin.msg(player, "too-big"); return; }
            if (player.getPassengers().contains(pet)) player.removePassenger(pet);
            else { if (player.getVehicle() == pet) player.leaveVehicle(); player.addPassenger(pet); }
        }
        player.closeInventory();
    }

    /** Ranura libre: pon cualquier objeto (el del cursor o el de la mano); clic derecho o Shift lo quita. */
    private void setSlot(PetData d, String key, InventoryClickEvent e) {
        ItemStack held = e.getCursor();
        if (held == null || held.getType().isAir()) held = player.getInventory().getItemInMainHand();
        if (e.isRightClick() || e.isShiftClick() || (held.getType().isAir() && d.custom.containsKey(key))) {
            d.custom.remove(key);
            if (key.equals("hand") || key.equals("head")) d.extra.put(key, 0);
            changed();
            return;
        }
        if (!plugin.getConfig().getBoolean("cosmetics.any-item", true)) { plugin.msg(player, "no-perm"); return; }
        if (held.getType().isAir()) { plugin.msg(player, "hold-item"); return; }
        d.custom.put(key, Cosmetics.asCosmetic(held));
        changed();
    }

    @Override
    public void click(InventoryClickEvent e) {
        if (data(petId) == null) return;
        Consumer<InventoryClickEvent> a = actions.get(e.getRawSlot());
        if (a != null) a.accept(e);
    }

    private void changed() {
        plugin.manager().save(player.getUniqueId());
        plugin.manager().refresh(player);
        build();
    }
}
