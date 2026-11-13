package dev.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Home {
    public String name;
    public String world;
    public double x, y, z;
    public float yaw, pitch;
    /** Material name, or null for the default bed icon. */
    public String icon;

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }
}
