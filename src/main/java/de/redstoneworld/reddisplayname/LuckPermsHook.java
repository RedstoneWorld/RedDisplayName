package de.redstoneworld.reddisplayname;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.entity.Player;

class LuckPermsHook {
    private final EventSubscription<UserDataRecalculateEvent> handler;
    private final LuckPerms lpApi;
    private RedDisplayName plugin;

    public LuckPermsHook(RedDisplayName plugin) {
        this.plugin = plugin;
        lpApi = LuckPermsProvider.get();
        handler = lpApi.getEventBus().subscribe(UserDataRecalculateEvent.class, this::onPermissionChange);
    }

    public void onPermissionChange(UserDataRecalculateEvent event) {
        Player player = plugin.getServer().getPlayer(event.getUser().getUniqueId());
        if(player != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.colorName(player));
        }
    }

    public void unregister() {
        if (handler != null) {
            handler.close();
        }
    }
}
