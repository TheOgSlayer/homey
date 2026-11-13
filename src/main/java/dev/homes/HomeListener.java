package dev.homes;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HomeListener implements Listener {
    private static final Pattern PAIR = Pattern.compile("(\\w+)\\s*:\\s*(?:\"([^\"]*)\"|'([^']*)')");

    private final HomesPlugin plugin;

    public HomeListener(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    private static Map<String, String> parse(BinaryTagHolder tag) {
        Map<String, String> out = new HashMap<>();
        if (tag == null) return out;
        Matcher m = PAIR.matcher(tag.string());
        while (m.find()) out.put(m.group(1), m.group(2) != null ? m.group(2) : m.group(3));
        return out;
    }

    @EventHandler
    public void onClick(PlayerCustomClickEvent event) {
        if (!event.getIdentifier().equals(HomeMenus.ACTION)) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection conn)) return;
        Player p = conn.getPlayer();
        Map<String, String> data = parse(event.getTag());
        DialogResponseView view = event.getDialogResponseView();
        String search = view == null ? null : view.getText("search");
        String newName = view == null ? null : view.getText("name");
        plugin.getServer().getScheduler().runTask(plugin, () -> handle(p, data, search, newName));
    }

    private void msg(Player p, String text, NamedTextColor color) {
        p.sendMessage(Component.text(text, color));
    }

    private void handle(Player p, Map<String, String> d, String search, String newName) {
        String a = d.getOrDefault("a", "");
        int slot;
        try { slot = Integer.parseInt(d.getOrDefault("s", "0")); } catch (NumberFormatException e) { slot = 0; }
        HomeMenus menus = plugin.menus();
        HomeManager homes = plugin.homes();
        if (slot < 0 || slot > plugin.homes().limit(p)) return;
        Home home = homes.get(p.getUniqueId(), slot);

        switch (a) {
            case "new" -> {
                if (slot >= 1 && home == null) {
                    Home created = homes.create(p, slot);
                    msg(p, created.name + " set.", NamedTextColor.GREEN);
                }
                if (plugin.getConfig().getBoolean("keep-gui-open-after-set", true)) menus.openList(p);
                else p.closeDialog();
            }
            case "open" -> menus.openHome(p, slot);
            case "tp" -> {
                p.closeDialog();
                if (home == null) return;
                Location loc = home.toLocation();
                if (loc == null) { msg(p, "That home's world is not loaded.", NamedTextColor.RED); return; }
                p.teleport(loc);
                msg(p, "Teleported to " + home.name + ".", NamedTextColor.GREEN);
            }
            case "icons" -> menus.openIcons(p, slot, "");
            case "search" -> menus.openIcons(p, slot, search);
            case "default" -> {
                if (home != null) { home.icon = null; homes.save(); }
                menus.openHome(p, slot);
            }
            case "seticon" -> {
                if (home != null) {
                    Material m = Material.matchMaterial(d.getOrDefault("m", ""));
                    if (m != null) { home.icon = m.getKey().getKey(); homes.save(); }
                }
                menus.openHome(p, slot);
            }
            case "rename" -> menus.openRename(p, slot);
            case "renamesave" -> {
                if (home != null && newName != null) {
                    String n = newName.strip().replaceAll("\\p{Cntrl}", "");
                    if (n.length() > 24) n = n.substring(0, 24);
                    if (!n.isEmpty()) { home.name = n; homes.save(); }
                }
                menus.openHome(p, slot);
            }
            case "delask" -> menus.openDelete(p, slot);
            case "del" -> {
                if (home != null) {
                    homes.remove(p.getUniqueId(), slot);
                    msg(p, home.name + " deleted.", NamedTextColor.RED);
                }
                menus.openList(p);
            }
            default -> { }
        }
    }
}
