package me.mascotasx;

/** Aislado para que PlaceholderAPI sea opcional: esta clase solo se carga si el plugin existe. */
final class PapiHook {
    private PapiHook() {}

    static Runnable register(MascotasX plugin) {
        PapiExpansion ex = new PapiExpansion(plugin);
        ex.register();
        return () -> ex.unregister();
    }
}
