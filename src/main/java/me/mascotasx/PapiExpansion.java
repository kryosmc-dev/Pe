package me.mascotasx;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

/**
 * Placeholders de la mascota invocada del jugador:
 * %mascotas_active%, %mascotas_type%, %mascotas_level%, %mascotas_max_level%, %mascotas_xp%,
 * %mascotas_xp_needed%, %mascotas_xp_percent%, %mascotas_damage%, %mascotas_mode%, %mascotas_owned%.
 */
public class PapiExpansion extends PlaceholderExpansion {
    private final MascotasX plugin;

    public PapiExpansion(MascotasX plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "mascotas"; }
    @Override public String getAuthor() { return "MascotasX"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        try {
            PetManager m = plugin.manager();
            Profile pr = m.peek(player.getUniqueId()); // solo jugadores conectados (no carga archivos)
            if (pr == null) return "";
            String key = params.toLowerCase(Locale.ROOT);
            if (key.equals("owned")) return String.valueOf(pr.owned.size());
            if (key.equals("max_level")) return String.valueOf(m.maxLevel());
            PetData d = pr.active == null ? null : pr.owned.get(pr.active);
            PetType t = d == null ? null : m.type(d.type);
            if (d == null || t == null) {
                return switch (key) {
                    case "active", "type", "mode" -> plugin.lang().s("words.papi-none");
                    default -> "0";
                };
            }
            return switch (key) {
                case "active" -> d.displayName(t);
                case "type" -> t.name();
                case "level" -> String.valueOf(d.level);
                case "xp" -> String.valueOf((long) d.xp);
                case "xp_needed" -> d.level >= m.maxLevel() ? "0" : String.valueOf((long) m.need(d.level));
                case "xp_percent" -> d.level >= m.maxLevel() ? "100"
                        : String.valueOf((int) Math.min(100, d.xp * 100.0 / Math.max(1, m.need(d.level))));
                case "damage" -> Text.num(m.damage(t, d));
                case "mode" -> plugin.lang().s("words.mode-" + d.mode.name().toLowerCase(Locale.ROOT));
                default -> null;
            };
        } catch (Exception ex) {
            return "";
        }
    }
}
