package net.teamshadowmc.shadowlist;

import net.teamshadowmc.shadowlist.utils.MySQL;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import javax.print.DocFlavor;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ShadowList extends JavaPlugin implements Listener {

    public static YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/ShadowList/config.yml"));

    public static String PluginName = "ShadowList";
    public static String PluginVersion;
    public static String SQLClosed =  String.format("[%s] MySQL connection was closed. Reopening.", PluginName);


    private static MySQL mysql;

    private static String SQLDatabase;
    private static String SQLTable;
    private static String SQLHostname;
    private static String SQLPort;
    private static String SQLUser;
    private static String SQLPass;

    private static int rowSize;

    private boolean wlEnabled = true;

    private List<String> wlist_players;

    private BukkitRunnable updater;

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        createConfig();

        if (getConfig().getBoolean("whitelist.enabled")) {
            wlEnabled = true;
        }
        reloadConfig();
        SQLDatabase = getConfig().getString("MySQL.database");
        SQLTable = getConfig().getString("MySQL.table");
        SQLHostname = getConfig().getString("MySQL.hostname");
        SQLPort= getConfig().getString("MySQL.port");
        SQLUser = getConfig().getString("MySQL.username");
        SQLPass = getConfig().getString("MySQL.password");
        rowSize = getConfig().getInt("misc.rowsize");
        try {
            mysql = new MySQL(SQLHostname,  SQLPort, SQLDatabase, SQLUser, SQLPass);
            mysql.openConnection();
            createTable();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void onDisable() {
        try {
            if (mysql.checkConnection()) {
                mysql.closeConnection();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    public void reloadConf() {
        reloadConfig();
        SQLDatabase = getConfig().getString("MySQL.database");
        SQLTable = getConfig().getString("MySQL.table");
        SQLHostname = getConfig().getString("MySQL.hostname");
        SQLPort= getConfig().getString("MySQL.port");
        SQLUser = getConfig().getString("MySQL.username");
        SQLPass = getConfig().getString("MySQL.password");
        rowSize = getConfig().getInt("misc.rowsize");

        createTable();
    }
    private void createTable() {
        final String createSQL = String.format("CREATE TABLE IF NOT EXISTS `%s` (`id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,`uuid` varchar(36) DEFAULT NULL,`name` varchar(36) NOT NULL, PRIMARY KEY (`id`), UNIQUE KEY `id` (`id`)) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8", SQLTable);
        final BukkitScheduler localScheduler = getServer().getScheduler();
        localScheduler.runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!mysql.checkConnection()) {
                        getLogger().warning(SQLClosed);
                        mysql.openConnection();
                    }
                    mysql.updateSQL(createSQL);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }
    private void createConfig() {
        boolean exists = new File("plugins/ShadowList/config.yml").exists();

        if (!exists) {
            new File("plugins/ShadowList").mkdir();
            config.options().header("ShadowList: Made by Fayettemat (some code by Huskey)");
            config.set("MySQL.hostname", "localhost");
            config.set("MySQL.port", "3306");
            config.set("MySQL.database", "shadowlist");
            config.set("MySQL.table", "mc_whitelist");
            config.set("MySQL.username", "shadowlist");
            config.set("MySQL.password", "password");
            config.set("misc.rowsize", 5);
            config.set("whitelist.enabled", false);
            config.set("whitelist.kickmessage", "&4You're not whitelisted on this server. Please visit website.com to apply.");
            config.set("config.version", 1);
            try {
                config.save("plugins/ShadowList/config.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            int cfgVer = getConfig().getInt("config.version");
            if (cfgVer > 1) {
                config.set("config.version", 1);
                try {
                    config.save("plugins/ShadowList/config.yml");
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

    }
    public void addToWhitelistPlayer(final CommandSender sender, final String target) {
            final CommandSender reply = sender;
            final BukkitScheduler localScheduler = getServer().getScheduler();
            localScheduler.runTaskAsynchronously(this, new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (!mysql.checkConnection()) {
                            getLogger().warning(SQLClosed);
                            mysql.openConnection();
                        }
                        String insert = String.format("INSERT INTO `%s` (`name`) VALUES ('%s');", SQLTable, target);
                        mysql.updateSQL(insert);
                        reply.sendMessage(String.format("%s added to whitelist", target));
                    } catch (SQLException ex) {
                            reply.sendMessage(String.format("Error adding %s to whitelist. See console for details.", target));
                            ex.printStackTrace();
                    } catch (ClassNotFoundException ex) {
                        reply.sendMessage(String.format("Error adding %s to whitelist. See log for details.", target));
                        ex.printStackTrace();
                    }
                }
            });
    }
    public void removeFromWhitelist(final CommandSender sender, final String target) {
        final CommandSender reply = sender;
        final BukkitScheduler localScheduler = getServer().getScheduler();
        final String db = SQLDatabase;
        localScheduler.runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!mysql.checkConnection()) {
                        getLogger().warning(SQLClosed);
                        mysql.openConnection();
                    }
                    String checkQuery = String.format("SELECT * FROM `%s` WHERE name = '%s'", SQLTable, target);
                    ResultSet checkRS = mysql.querySQL(checkQuery);
                    if (checkRS.isBeforeFirst()) {
                        checkRS.next();
                        if (checkRS.getString("name") != null) {
                            String removeQuery = String.format("DELETE FROM %s WHERE name = '%s'", SQLTable, target);
                            mysql.updateSQL(removeQuery);
                            reply.sendMessage(String.format("[%s] Removed %s from %s.", PluginName, SQLTable, target));
                        }
                    } else {
                        reply.sendMessage(String.format("[%s] Failed to remove %s. Are you sure they are whitelisted and you spelled it right?", PluginName, target));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                    getLogger().severe(String.format("[%s] You are missing a required class file! See above stacktrace for more info.", PluginName));
                }
            }
        });
    }
    public void checkWhitelist(final CommandSender sender, final String target) {
        final CommandSender reply = sender;
        final BukkitScheduler localScheduler = getServer().getScheduler();
        localScheduler.runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!mysql.checkConnection()) {
                        getLogger().warning(SQLClosed);
                        mysql.openConnection();
                    }
                    String checkQuery = String.format("SELECT * FROM `%s` WHERE name = '%s'", SQLTable, target);
                    ResultSet checkRS = mysql.querySQL(checkQuery);
                    if (checkRS.isBeforeFirst()) {
                        checkRS.next();
                        if (checkRS.getString("name") != null) {
                            reply.sendMessage(String.format("[%s] %s is on the whitelist.", PluginName, target));
                        }
                        else {
                            reply.sendMessage(String.format("[%s] %s is not on the whitelist.", PluginName, target));
                        }
                    } else {
                        reply.sendMessage(String.format("[%s] %s is not on the whitelist.", PluginName, target));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                    getLogger().severe(String.format("[%s] You are missing a required class file! See above stacktrace for more info.", PluginName));
                }
            }
        });
    }
    public void listWhitelist(final CommandSender sender) {
        final BukkitScheduler localScheduler = getServer().getScheduler();
        localScheduler.runTaskAsynchronously(this, new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!mysql.checkConnection()) {
                        getLogger().warning(SQLClosed);
                        mysql.openConnection();
                    }

                    ResultSet select = mysql.querySQL(String.format("SELECT * FROM `%s`.`%s`;", SQLDatabase, SQLTable));

                    if (select.isBeforeFirst()) {
                        sender.sendMessage(ChatColor.GOLD + "--- Whitelisted Players ---");
                        int perRow = rowSize-1;
                        int rowCount = 0;
                        int totalCount = 0;
                        String whitelistRow = "";

                        while (select.next()) {
                            if (rowCount == perRow || select.isLast()) {
                                whitelistRow = whitelistRow + select.getString("name");
                                sender.sendMessage(ChatColor.DARK_GREEN + whitelistRow);
                                rowCount = 0;
                                whitelistRow = "";
                            } else {
                                whitelistRow = whitelistRow + select.getString("name") + ", ";
                            }
                            rowCount++;
                            totalCount++;
                        }
                        sender.sendMessage(ChatColor.GOLD + "--- " + totalCount + " Whitelisted Players ---");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (commandLabel.equalsIgnoreCase("wl")) {
            if ((sender instanceof Player && sender.hasPermission("shadowlist.admin")) || !(sender instanceof Player)) {
                if (args.length > 0) {
                    String para = args[0];
                    if (args.length == 2) {
                        if (para.equalsIgnoreCase("add")) {
                            addToWhitelistPlayer(sender, args[1]);
                        } else if (para.equalsIgnoreCase("remove")) {
                            removeFromWhitelist(sender, args[1]);
                        } else if (para.equalsIgnoreCase("check")) {
                            checkWhitelist(sender, args[1]);
                        }
                    } else if (para.equalsIgnoreCase("list")) {
                        listWhitelist(sender);
                    } else if (para.equalsIgnoreCase("reload")) {
                        getLogger().info(String.format("[%s] Reloading Config.", PluginName));
                        reloadConf();
                    } else if (para.equalsIgnoreCase("help")) {
                        displayHelp(sender);
                    } else {
                        sender.sendMessage("[ShadowList] Please use /wl help for help");
                    }
                } else {
                    sender.sendMessage("[ShadowList] Please use /wl help for help");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "[ShadowList] You do not have permission for this command.");
                return false;
            }
        }
        return false;
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String joiningUUID = p.getUniqueId().toString();
        /*
        if (isWhiteListed( {
            p.kickPlayer(ChatColor.translateAlternateColorCodes('&', config.getString("kickMessage")));
        }
        */
    }

    @EventHandler
    public void preLogin(AsyncPlayerPreLoginEvent event) {
        String joiningUUID = event.getUniqueId().toString();
        String joiningPlayer = event.getName();
        if (wlEnabled) {
            try {
                if (!mysql.checkConnection()) {
                    getLogger().warning(SQLClosed);
                    mysql.openConnection();
                }
                String queryPlayer = String.format("SELECT uuid, name FROM `%s`.`whitelist` WHERE name = '%s'", config.getString("MySQL.database"), joiningPlayer);
                ResultSet resultPlayer = mysql.querySQL(queryPlayer);

                if (resultPlayer.isBeforeFirst()) {

                    resultPlayer.next();
                    if (resultPlayer.getString("uuid") != null) {
                        if (joiningUUID.equalsIgnoreCase(resultPlayer.getString("uuid"))) {
                            if (!joiningPlayer.equalsIgnoreCase(resultPlayer.getString("name"))) {
                                try {
                                    String updateName = String.format("UPDATE `%s`.`whitelist` SET `name` = '%s' WHERE `uuid` = '%s'", config.getString("MySQL.database"), joiningPlayer, joiningUUID);
                                    mysql.updateSQL(updateName);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                } catch (ClassNotFoundException ex) {
                                    return;
                                }
                            }
                        }
                    } else if (resultPlayer.getString("name").equalsIgnoreCase(joiningPlayer)) {
                        if (resultPlayer.getString("uuid") == null) {
                            try {
                                String insertUUID = String.format("UPDATE `%s`.`whitelist` SET `uuid` = '%s' WHERE `name` = '%s'", config.getString("MySQL.database"), joiningUUID, joiningPlayer);
                                mysql.updateSQL(insertUUID);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            } catch (ClassNotFoundException ex) {
                                return;
                            }
                        }
                    } else {
                        event.setKickMessage(ChatColor.translateAlternateColorCodes('&', config.getString("whitelist.kickmessage").replace("\\n", "\n")));
                        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
                    }
                } else {
                    event.setKickMessage(ChatColor.translateAlternateColorCodes('&', config.getString("whitelist.kickmessage").replace("\\n", "\n")));
                    event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                event.setKickMessage(ChatColor.RED+"There is an issue verifying your status. Please contact an admin.");
                //event.setKickMessage(ChatColor.translateAlternateColorCodes('&', config.getString("whitelist.kickmessage")));
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                event.setKickMessage(ChatColor.RED+"There is an issue verifying your status. Please contact an admin.");
                //event.setKickMessage(ChatColor.translateAlternateColorCodes('&', config.getString("whitelist.kickmessage")));
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
            }
        }
    }

    private void displayHelp(CommandSender sender) {
        sender.sendMessage("[ShadowList] /wl add player    | Add a player to the whitelist");
        sender.sendMessage("[ShadowList] /wl remove player | Remove player from whitelist");
        sender.sendMessage("[ShadowList] /wl check player  | Check if a player is on whitelist ");
        sender.sendMessage("[ShadowList] /wl list          | List whitelisted players");
    }
}
