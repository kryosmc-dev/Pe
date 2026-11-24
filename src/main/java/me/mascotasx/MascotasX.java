package me.mascotasx;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MascotasX extends JavaPlugin {
    private static MascotasX instance;
    private PetManager manager;
    private EconomyHook economy;
    private PetListener listener;
    private Lang lang;
    private Runnable papiOff;
    private final java.util.Set<String> disabledWorlds = new java.util.HashSet<>();

    public static MascotasX get() { return instance; }
    public PetManager manager() { return manager; }
    public EconomyHook economy() { return economy; }
    public PetListener listener() { return listener; }
    public Lang lang() { return lang; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        lang = new Lang(this);
        lang.load();
        loadSettings();
        if (!new File(getDataFolder(), "pets.yml").exists()) saveResource("pets.yml", false);

        setupEconomy();

        manager = new PetManager(this);
        manager.loadTypes();
        registerExtras();
        manager.startTasks();

        listener = new PetListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        PetCommand cmd = new PetCommand(this);
        getCommand("mascotas").setExecutor(cmd);
        getCommand("mascotas").setTabCompleter(cmd);

        Cosmetics.resetGlowTeams();
        for (Player p : Bukkit.getOnlinePlayers()) listener.handleJoin(p);

        // Los proveedores de economia (EssentialsX, CMI...) pueden cargar despues de este plugin: se reintenta al terminar de arrancar el servidor
        if (economy == null && economyExpected()) getServer().getScheduler().runTask(this, () -> setupEconomy(true));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                papiOff = PapiHook.register(this);
                getLogger().info("PlaceholderAPI detectado: placeholders %mascotas_*% disponibles.");
            } catch (Throwable t) {
                getLogger().warning("No se pudo registrar PlaceholderAPI: " + t.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        if (papiOff != null) { try { papiOff.run(); } catch (Throwable ignored) { } }
        if (manager != null) manager.shutdown();
    }

    /** Lee los ajustes de config.yml que se consultan a menudo (mundos sin mascotas). */
    public void loadSettings() {
        disabledWorlds.clear();
        for (String w : getConfig().getStringList("disabled-worlds")) disabledWorlds.add(w.toLowerCase(java.util.Locale.ROOT));
    }

    /** false si ese mundo esta en disabled-worlds (config.yml). */
    public boolean worldAllowed(org.bukkit.World w) {
        return w == null || !disabledWorlds.contains(w.getName().toLowerCase(java.util.Locale.ROOT));
    }

    /** true si config.yml pide economia y Vault esta instalado (entonces "sin economia" es un fallo, no un servidor gratuito). */
    public boolean economyExpected() {
        return getConfig().getBoolean("economy.enabled", true) && getServer().getPluginManager().getPlugin("Vault") != null;
    }

    /** (Re)enlaza Vault segun config.yml (economy.enabled). */
    public void setupEconomy() { setupEconomy(false); }

    /** quiet=true: reintento silencioso (arranque tardio del proveedor de economia). */
    public void setupEconomy(boolean quiet) {
        economy = null;
        if (economyExpected()) {
            try {
                VaultHook hook = new VaultHook();
                if (hook.setup(this)) {
                    economy = hook;
                    if (quiet) getLogger().info("Economia (Vault) enlazada.");
                }
            } catch (Throwable t) {
                getLogger().warning("No se pudo enlazar con Vault: " + t.getMessage());
            }
        }
        if (economy == null && !quiet) {
            if (economyExpected()) getLogger().warning("Vault esta instalado pero aun no hay proveedor de economia: se reintentara al arrancar y las compras se bloquean mientras tanto.");
            else getLogger().warning("Sin economia (Vault): las mascotas se entregaran gratis.");
        }
    }

    public void reload() {
        reloadConfig();
        lang.load();
        loadSettings();
        setupEconomy();
        manager.loadTypes();
        registerExtras();
    }

    /** Escribe en lang.yml los nombres y opciones de los cosmeticos unicos de cada mob para poder editarlos. */
    private void registerExtras() {
        try {
            for (PetType t : manager.types())
                for (Cosmetics.Extra x : Cosmetics.extras(t)) {
                    String base = "extras." + t.entity().name() + "." + x.key();
                    lang.auto(base + ".name", x.name());
                    lang.autoList(base + ".labels", x.labels());
                }
            lang.flush();
        } catch (Throwable ex) {
            getLogger().warning("No se pudieron registrar los cosmeticos en lang.yml: " + ex.getMessage());
        }
    }

    /** Icono personalizable (icons.<key> en config.yml, editable desde /mascotas admin). */
    public org.bukkit.Material icon(String key, org.bukkit.Material def) {
        String v = getConfig().getString("icons." + key);
        org.bukkit.Material m = v == null ? null : org.bukkit.Material.matchMaterial(v);
        return m == null || !m.isItem() ? def : m;
    }

    /** Envia un mensaje de lang.yml (messages.<key>) reemplazando pares %clave% -> valor. */
    public void msg(CommandSender to, String key, String... kv) {
        to.sendMessage(Text.c(lang.s("messages.prefix") + lang.s("messages." + key, kv)));
    }
}
