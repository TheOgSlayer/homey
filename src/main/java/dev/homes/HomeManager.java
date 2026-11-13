package dev.homes;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class HomeManager {
    private final HomesPlugin plugin;
    private final File file;
    private final Map<UUID, TreeMap<Integer, Home>> data = new HashMap<>();

    public HomeManager(HomesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;
        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { continue; }
            ConfigurationSection slots = players.getConfigurationSection(uuidStr);
            if (slots == null) continue;
            TreeMap<Integer, Home> map = new TreeMap<>();
            for (String slotStr : slots.getKeys(false)) {
                ConfigurationSection s = slots.getConfigurationSection(slotStr);
                if (s == null) continue;
                try {
                    Home h = new Home();
                    h.name = s.getString("name", "Home " + slotStr);
                    h.world = s.getString("world", "world");
                    h.x = s.getDouble("x");
                    h.y = s.getDouble("y");
                    h.z = s.getDouble("z");
                    h.yaw = (float) s.getDouble("yaw");
                    h.pitch = (float) s.getDouble("pitch");
                    h.icon = s.getString("icon", null);
                    map.put(Integer.parseInt(slotStr), h);
                } catch (NumberFormatException ignored) { }
            }
            data.put(uuid, map);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, TreeMap<Integer, Home>> pe : data.entrySet()) {
            for (Map.Entry<Integer, Home> he : pe.getValue().entrySet()) {
                String p = "players." + pe.getKey() + "." + he.getKey();
                Home h = he.getValue();
                yaml.set(p + ".name", h.name);
                yaml.set(p + ".world", h.world);
                yaml.set(p + ".x", h.x);
                yaml.set(p + ".y", h.y);
                yaml.set(p + ".z", h.z);
                yaml.set(p + ".yaw", h.yaw);
                yaml.set(p + ".pitch", h.pitch);
                yaml.set(p + ".icon", h.icon);
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    public Home get(UUID id, int slot) {
        TreeMap<Integer, Home> m = data.get(id);
        return m == null ? null : m.get(slot);
    }

    public Home create(Player p, int slot) {
        Home h = new Home();
        h.name = "Home " + slot;
        Location l = p.getLocation();
        h.world = l.getWorld().getName();
        h.x = l.getX();
        h.y = l.getY();
        h.z = l.getZ();
        h.yaw = l.getYaw();
        h.pitch = l.getPitch();
        h.icon = null;
        data.computeIfAbsent(p.getUniqueId(), k -> new TreeMap<>()).put(slot, h);
        save();
        return h;
    }

    public void remove(UUID id, int slot) {
        TreeMap<Integer, Home> m = data.get(id);
        if (m != null) {
            m.remove(slot);
            save();
        }
    }

    /** Slots available to the player: config max-homes, or the highest homes.limit.N permission. */
    public int limit(Player p) {
        int max = Math.max(1, plugin.getConfig().getInt("max-homes", 3));
        for (int i = 100; i > max; i--) {
            if (p.hasPermission("homes.limit." + i)) return i;
        }
        return max;
    }
}
