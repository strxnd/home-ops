package dev.glitg.smp;

import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

/** Persistent, intentionally small state store. Values are absolute epoch milliseconds. */
final class SeasonState {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration yaml;

    SeasonState(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
        reload();
    }
    void reload() { yaml = YamlConfiguration.loadConfiguration(file); }
    void save() { try { yaml.save(file); } catch (Exception e) { plugin.getLogger().severe("Could not save state.yml: " + e.getMessage()); } }
    String phase() { return yaml.getString("season.phase", "PRESEASON"); }
    void phase(String phase) { yaml.set("season.phase", phase); save(); }
    long getLong(String path) { return yaml.getLong(path, 0L); }
    void setLong(String path, long value) { yaml.set(path, value); save(); }
    boolean getBoolean(String path) { return yaml.getBoolean(path, false); }
    void setBoolean(String path, boolean value) { yaml.set(path, value); save(); }
    String getString(String path) { return yaml.getString(path); }
    void setString(String path, String value) { yaml.set(path, value); save(); }
    long protectionUntil(UUID player) { return getLong("protection." + player); }
    void protectionUntil(UUID player, long until) { yaml.set("protection." + player, until); save(); }
    long combatUntil(UUID player) { return getLong("combat." + player); }
    void combatUntil(UUID player, long until) { yaml.set("combat." + player, until); save(); }
    long recoveryAt(UUID itemEntity) { return getLong("recovery." + itemEntity); }
    void recoveryAt(UUID itemEntity, long when) { yaml.set("recovery." + itemEntity, when); save(); }
    void clearRecovery(UUID itemEntity) { yaml.set("recovery." + itemEntity, null); save(); }
}
