package de.redstoneworld.reddisplayname;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class RedDisplayName extends JavaPlugin implements Listener {
    
    private boolean onlyColors;
    private boolean showPrefix;
    private boolean showSuffix;
    
    private Chat vaultChat;
    private LuckPermsHook lpHook;
    
    @Override
    public void onEnable() {
        loadConfig();
        getCommand("reddisplayname").setExecutor(this);
        
        if (!setupVaultChat()) {
            getLogger().severe("Failed to setup Vault chat! The plugin will not enable!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            getLogger().info("Detected LuckPerms");
            if (lpHook != null) {
                lpHook.unregister();
            }
            lpHook = new LuckPermsHook(this);
        }
        
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    private boolean setupVaultChat() {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(Chat.class);
        if (chatProvider != null) {
            vaultChat = chatProvider.getProvider();
        }
        return vaultChat != null;
    }
    
    @Override
    public void onDisable() {
        lpHook.unregister();
        for(Team team : getServer().getScoreboardManager().getMainScoreboard().getTeams()) {
            if(team.getName().startsWith("rdn")) {
                team.unregister();
            }
        }
    }
    
    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        
        onlyColors = getConfig().getBoolean("only-colors");
        showPrefix = getConfig().getBoolean("show.prefix");
        showSuffix = getConfig().getBoolean("show.suffix");
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            colorName(event.getPlayer());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Team team = getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(event.getPlayer().getName());
        if (team != null && team.getName().startsWith("tc")) {
            team.removeEntry(event.getPlayer().getName());
            if (team.getSize() <= 0) {
                team.unregister();
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        colorName(event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("reddisplayname.command.reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;
            }
        }
        return false;
    }
    
    public void colorName(Player player) {
        if (!isEnabled()) {
            return;
        }
        Scoreboard scoreboard = getServer().getScoreboardManager().getMainScoreboard();
        Team playerTeam = scoreboard.getEntryTeam(player.getName());
        if (playerTeam != null && !playerTeam.getName().startsWith("rdn")) {
            // Player already in non-tag team, don't switch team to not break events and shit
            return;
        }
        PermissionGroup appliedGroup = getAppliedGroup(player);
        String permPrefix = getPrefix(player);
        String permSuffix = getSuffix(player);
        String playerName = player.getName();
        
        if (!permPrefix.isEmpty() || !permSuffix.isEmpty()) {
            if (permPrefix.length() > 16)
                permPrefix = permPrefix.substring(0, 16);
                
            
            String teamName = "rdn" + (appliedGroup.getWeight().isEmpty() ? appliedGroup.getName().substring(0, 2) : appliedGroup.getWeight()) + permPrefix.replace(' ', '_') + permSuffix.replace(' ', '_');
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }
            if (playerTeam == null || !playerTeam.getName().equals(teamName)) {
                Team team = scoreboard.getTeam(teamName);
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                    team.setPrefix(permPrefix);
                    team.setSuffix(permSuffix);
                    team.setCanSeeFriendlyInvisibles(false);
                    for (int i = permPrefix.length(); i > 1; i--) {
                        if (permPrefix.charAt(i - 1) == ChatColor.COLOR_CHAR) {
                            ChatColor color = ChatColor.getByChar(permPrefix.charAt(i));
                            if (color != null && color.isColor()) {
                                team.setColor(color);
                                break;
                            }
                        }
                    }
                }
                team.addEntry(playerName);
            }
            
            if (playerTeam != null && playerTeam.getSize() == 0) {
                playerTeam.unregister();
            }
        } else if (scoreboard.getEntryTeam(player.getName()) != null && scoreboard.getEntryTeam(player.getName()).getName().startsWith("rdn")) {
            scoreboard.getEntryTeam(player.getName()).removeEntry(player.getName());
        }
    }
    
    /**
     * get the group that applies to this player
     * @param player The player to get the group for
     * @return PermissionGroup The top group the player is in
     */
    private PermissionGroup getAppliedGroup(Player player) {
        String groupName = vaultChat.getPrimaryGroup(player);
        String groupWeight = vaultChat.getGroupInfoString(player.getWorld(), groupName, "weight", "");
        
        return new PermissionGroup(groupName, groupWeight);
    }
    
    /**
     * Get the prefix of a user
     * @param player The player to get the prefix for
     * @return The prefix, empty string if he doesn't have one
     */
    private String getPrefix(Player player) {
        if (showPrefix) {
            
            String prefix = vaultChat.getPlayerInfoString(player, "reddisplayname.prefix", null);
            if (prefix == null || prefix.isEmpty()) {
                prefix = vaultChat.getPlayerPrefix(player);
            }
            if (prefix == null || prefix.isEmpty()) {
                String group = vaultChat.getPrimaryGroup(player);
                prefix = vaultChat.getGroupInfoString(player.getWorld(), group, "reddisplayname.prefix", null);
                if (prefix == null || prefix.isEmpty()) {
                    prefix = vaultChat.getGroupPrefix(player.getWorld(), group);
                }
            }
            return prefix != null ? handleColors(prefix) : "";
        }
        return "";
    }
    
    /**
     * Get the suffix of a user
     * @param player The player to get the suffix for
     * @return The suffix, empty string if he doesn't have one
     */
    private String getSuffix(Player player) {
        if (showSuffix) {
            String suffix = vaultChat.getPlayerInfoString(player, "reddisplayname.suffix", null);
            if (suffix == null || suffix.isEmpty()) {
                suffix = vaultChat.getPlayerSuffix(player);
            }
            if (suffix == null || suffix.isEmpty()) {
                String group = vaultChat.getPrimaryGroup(player);
                suffix = vaultChat.getGroupInfoString(player.getWorld(), group, "reddisplayname.suffix", null);
                if (suffix == null || suffix.isEmpty()) {
                    suffix = vaultChat.getGroupSuffix(player.getWorld(), group);
                }
            }
            return suffix != null ? handleColors(suffix) : "";
        }
        return "";
    }
    
    /**
     * Translate, check and strip colors
     * @param string The string to translate, check and strip colors from
     * @return Only colors if only-colors is enabled in the config
     */
    private String handleColors(String string) {
        string = ChatColor.translateAlternateColorCodes('&', string);
        if (onlyColors) {
            return ChatColor.getLastColors(string);
        }
        return string;
    }
}
