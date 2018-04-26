package de.redstoneworld.reddisplayname;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.event.EventHandler;
import me.lucko.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.entity.Player;

class LuckPermsHook {
    private final EventHandler<UserDataRecalculateEvent> handler;
    private final LuckPermsApi lpApi;
    private RedDisplayName plugin;

    public LuckPermsHook(RedDisplayName plugin) {
        this.plugin = plugin;
        lpApi = LuckPerms.getApi();
        handler = lpApi.getEventBus().subscribe(UserDataRecalculateEvent.class, this::onPermissionChange);
    }

    public void onPermissionChange(UserDataRecalculateEvent event) {
        Player player = plugin.getServer().getPlayer(event.getUser().getUuid());
        if(player != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.colorName(player));
        }
    }

    public void unregister() {
        if (handler != null) {
            handler.unregister();
        }
    }
}
