package dev.glitg.smp;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class SeasonState {
    private final JavaPlugin plugin;
    private final File file;
    private final FileConfiguration yaml;

    SeasonState(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "state.yml");
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    String phase() { return yaml.getString("season.phase", "PRESEASON"); }
    void phase(String value) { yaml.set("season.phase", value); save(); }
    long getLong(String path) { return yaml.getLong(path); }
    void setLong(String path, long value) { yaml.set(path, value); save(); }
    boolean getBoolean(String path) { return yaml.getBoolean(path); }
    void setBoolean(String path, boolean value) { yaml.set(path, value); save(); }
    long protectionUntil(UUID player) { return getLong("protection." + player); }
    void protectionUntil(UUID player, long value) { setLong("protection." + player, value); }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Unable to save state.yml: " + exception.getMessage());
        }
    }
}
