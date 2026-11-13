package dev.homes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HomesPlugin extends JavaPlugin {
    private HomeManager homes;
    private IconResolver icons;
    private HomeMenus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        homes = new HomeManager(this);
        icons = new IconResolver();
        icons.loadOverrides(getConfig().getConfigurationSection("icon-overrides"));
        menus = new HomeMenus(this);
        getServer().getPluginManager().registerEvents(new HomeListener(this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("homes.admin")) {
                sender.sendMessage("You do not have permission.");
                return true;
            }
            reloadConfig();
            icons.loadOverrides(getConfig().getConfigurationSection("icon-overrides"));
            sender.sendMessage("Homes config reloaded.");
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        menus.openList(p);
        return true;
    }

    public HomeManager homes() { return homes; }
    public IconResolver icons() { return icons; }
    public HomeMenus menus() { return menus; }
}
