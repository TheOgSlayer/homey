package dev.homes;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class HomeMenus {
    public static final Key ACTION = Key.key("homes", "act");
    private static final TextColor DIM = TextColor.color(0x6E6E6E);

    private final HomesPlugin plugin;

    public HomeMenus(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    private int cfg(String path, int def) {
        return plugin.getConfig().getInt("layout." + path, def);
    }

    private static BinaryTagHolder payload(String action, String... kv) {
        StringBuilder sb = new StringBuilder("{a:\"").append(action).append('"');
        for (int i = 0; i + 1 < kv.length; i += 2) {
            sb.append(',').append(kv[i]).append(":\"").append(kv[i + 1]).append('"');
        }
        return BinaryTagHolder.binaryTagHolder(sb.append('}').toString());
    }

    private static ActionButton button(Component label, int width, String action, String... kv) {
        return ActionButton.create(label, null, width, DialogAction.customClick(ACTION, payload(action, kv)));
    }

    private static Component white(String s) {
        return Component.text(s, NamedTextColor.WHITE);
    }

    private static DialogBody itemBody(Material m) {
        return DialogBody.item(new ItemStack(m == null ? Material.WHITE_BED : m))
                .showDecorations(false).showTooltip(false).build();
    }

    private static Material iconMaterial(Home h) {
        return h.icon == null ? null : Material.matchMaterial(h.icon);
    }

    /** /homes main menu. */
    public void openList(Player p) {
        int max = plugin.homes().limit(p);
        int width = cfg("home-button-width", 100);
        List<ActionButton> buttons = new ArrayList<>();
        for (int slot = 1; slot <= max; slot++) {
            Home h = plugin.homes().get(p.getUniqueId(), slot);
            if (h == null) {
                buttons.add(button(Component.text("New Home", DIM), width, "new", "s", String.valueOf(slot)));
            } else {
                Component label = Component.empty().color(NamedTextColor.WHITE)
                        .append(plugin.icons().icon(iconMaterial(h)))
                        .append(Component.text(" " + h.name));
                buttons.add(button(label, width, "open", "s", String.valueOf(slot)));
            }
        }
        int cols = Math.max(1, Math.min(max, cfg("home-columns", 3)));
        Dialog dialog = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(Component.text("HOMES"))
                        .body(List.of(itemBody(null)))
                        .build())
                .type(DialogType.multiAction(buttons).columns(cols).exitAction(null).build()));
        p.showDialog(dialog);
    }

    /** Menu for one home: Teleport / Change Icon / Rename / Delete. */
    public void openHome(Player p, int slot) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) { openList(p); return; }
        String s = String.valueOf(slot);
        int w = cfg("action-button-width", 120);
        List<ActionButton> buttons = List.of(
                button(white("Teleport"), w, "tp", "s", s),
                button(white("Change Icon"), w, "icons", "s", s),
                button(white("Rename"), w, "rename", "s", s),
                button(Component.text("Delete", NamedTextColor.RED), w, "delask", "s", s));
        Dialog dialog = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(Component.text(h.name))
                        .body(List.of(itemBody(iconMaterial(h))))
                        .build())
                .type(DialogType.multiAction(buttons).columns(2).exitAction(null).build()));
        p.showDialog(dialog);
    }

    /** Icon picker: search box, Search / Default / Back, then every item. */
    public void openIcons(Player p, int slot, String query) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) { openList(p); return; }
        String s = String.valueOf(slot);
        int w = cfg("icon-button-width", 140);
        List<IconResolver.Entry> results = plugin.icons().search(query);

        List<ActionButton> buttons = new ArrayList<>(results.size() + 3);
        buttons.add(button(white("Search"), w, "search", "s", s));
        buttons.add(button(white("Default"), w, "default", "s", s));
        buttons.add(button(Component.text("Back", NamedTextColor.RED), w, "open", "s", s));
        for (IconResolver.Entry e : results) {
            Component label = Component.empty().color(NamedTextColor.WHITE)
                    .append(plugin.icons().icon(e.material()))
                    .append(Component.text(" " + e.name()));
            buttons.add(button(label, w, "seticon", "s", s, "m", e.material().getKey().getKey()));
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(itemBody(iconMaterial(h)));
        if (results.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("No items found.", NamedTextColor.GRAY)));
        }
        DialogInput search = DialogInput.text("search", Component.text("Search"))
                .width(cfg("search-box-width", 220))
                .initial(query == null ? "" : query)
                .maxLength(64)
                .build();
        int cols = Math.max(1, cfg("icon-columns", 4));
        Dialog dialog = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(Component.text("Choose Icon"))
                        .body(body)
                        .inputs(List.of(search))
                        .build())
                .type(DialogType.multiAction(buttons).columns(cols).exitAction(null).build()));
        p.showDialog(dialog);
    }

    public void openRename(Player p, int slot) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) { openList(p); return; }
        String s = String.valueOf(slot);
        DialogInput name = DialogInput.text("name", Component.text("New Name"))
                .width(220)
                .initial(h.name)
                .maxLength(24)
                .build();
        ActionButton save = button(Component.text("Save", NamedTextColor.GREEN), 100, "renamesave", "s", s);
        ActionButton cancel = button(Component.text("Cancel", NamedTextColor.RED), 100, "open", "s", s);
        Dialog dialog = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(Component.text("Rename Home"))
                        .body(List.of(itemBody(iconMaterial(h))))
                        .inputs(List.of(name))
                        .build())
                .type(DialogType.confirmation(save, cancel)));
        p.showDialog(dialog);
    }

    public void openDelete(Player p, int slot) {
        Home h = plugin.homes().get(p.getUniqueId(), slot);
        if (h == null) { openList(p); return; }
        String s = String.valueOf(slot);
        ActionButton delete = button(Component.text("Delete", NamedTextColor.RED), 100, "del", "s", s);
        ActionButton cancel = button(white("Cancel"), 100, "open", "s", s);
        Dialog dialog = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(Component.text("Delete " + h.name + "?", NamedTextColor.RED))
                        .build())
                .type(DialogType.confirmation(delete, cancel)));
        p.showDialog(dialog);
    }
}
