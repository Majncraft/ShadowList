package net.teamshadowmc.shadowlist;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.teamshadowmc.shadowlist.utils.SQLHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class ShadowList extends JavaPlugin implements Listener {

    public static YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/ShadowList/config.yml"));

    public static String PluginName = ChatColor.BLUE+"ShadowList"+ChatColor.RESET;
    public static String PluginVersion;
    public static String SQLClosed =  String.format("[%s] MySQL connection was closed. Reopening.", PluginName);

    @Getter
    private static SQLHandler sqlHandler;

    private static String SQLDatabase;
    public static String SQLTable;
    private static String SQLHostname;
    private static int SQLPort;
    private static String SQLUser;
    private static String SQLPass;
    private static boolean SQLReconnect;

    private static int rowSize;

    private boolean wlEnabled = true;

    private List<String> wlist_players;

    private BukkitRunnable updater;

    private ConsoleCommandSender logger;

    public void onLoad() {
        logger = getServer().getConsoleSender();
    }

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
        SQLPort= Integer.parseInt(getConfig().getString("MySQL.port"));
        SQLUser = getConfig().getString("MySQL.username");
        SQLPass = getConfig().getString("MySQL.password");
        SQLReconnect = getConfig().getBoolean("MySQL.reconnect", true);
        rowSize = getConfig().getInt("misc.rowsize");

        logger.sendMessage(String.format("[%s] Connecting to MySQL...", PluginName));
        try {
            sqlHandler = SQLHandler.createMySQL(SQLHostname, SQLPort, SQLUser, SQLPass, SQLDatabase);
            sqlHandler.createTables();
        } catch (Exception e) {
            logger.sendMessage(String.format("[%s] [MySQL] Server is not responding or credentials are wrong. Check your MySQL credentials in config.yml file!", PluginName));
            setEnabled(false);
            return;
        }
    }

    public void onDisable() {
        /*if (sqlHandler != null) {
        try {
            sqlHandler.close();
            } catch(SQLException ex){
                ex.printStackTrace();
            }
        }*/
    }
    public void reloadConf() {
        reloadConfig();
        SQLDatabase = getConfig().getString("MySQL.database");
        SQLTable = getConfig().getString("MySQL.table");
        SQLHostname = getConfig().getString("MySQL.hostname");
        SQLPort= Integer.parseInt(getConfig().getString("MySQL.port"));
        SQLUser = getConfig().getString("MySQL.username");
        SQLPass = getConfig().getString("MySQL.password");
        SQLReconnect = getConfig().getBoolean("MySQL.reconnect", true);
        rowSize = getConfig().getInt("misc.rowsize");

        try {
            sqlHandler.createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            config.set("MySQL.reconnect", true);
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


    /*public void listWhitelist(final CommandSender sender) {
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
    */

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if ((sender instanceof Player && sender.hasPermission("shadowlist.admin")) || !(sender instanceof Player)) {
            if (commandLabel.equalsIgnoreCase("wl")) {
                if (args.length > 0) {
                    String para = args[0];
                    if (args.length == 2) {
                        if (para.equalsIgnoreCase("add")) {
                            try {
                                sqlHandler.addToWhitelist(sender, args[1]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (para.equalsIgnoreCase("remove")) {
                            try {
                                sqlHandler.removeFromWhitelist(sender, args[1]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (para.equalsIgnoreCase("check")) {
                            try {
                                sqlHandler.checkWhitelist(sender, args[1]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (para.equalsIgnoreCase("list")) {
                        //listWhitelist(sender);
                    } else if (para.equalsIgnoreCase("reload")) {
                        sender.sendMessage(String.format("[%s] Reloading Config.", PluginName));
                        reloadConf();
                    } else if (para.equalsIgnoreCase("help")) {
                        displayHelp(sender);
                    } else {
                        sender.sendMessage(String.format("[%s] Please use /wl help for help", PluginName));
                    }
                } else {
                    sender.sendMessage(String.format("[%s] Please use /wl help for help", PluginName));
                }
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
            return false;
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
    public void preLogin(AsyncPlayerPreLoginEvent event) throws Exception {
        String joiningUUID = event.getUniqueId().toString();
        String joiningPlayer = event.getName();
        if (wlEnabled) {
            String canJoin = sqlHandler.preLoginCheck(joiningPlayer, joiningUUID);
            if (canJoin == "no") {
                event.setKickMessage(ChatColor.translateAlternateColorCodes('&', ShadowList.config.getString("whitelist.kickmessage").replace("\\n", "\n")));
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
            } else if (canJoin == "error") {
                event.setKickMessage(ChatColor.RED+"There is an issue verifying your status. Please contact an admin.");
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);

            }
        }
    }

    private void displayHelp(CommandSender sender) {
        sender.sendMessage(String.format("[%s] /wl add player    | Add a player to the whitelist", PluginName));
        sender.sendMessage(String.format("[%s] /wl remove player | Remove player from whitelist", PluginName));
        sender.sendMessage(String.format("[%s] /wl check player  | Check if a player is on whitelist ", PluginName));
        sender.sendMessage(String.format("[%s] /wl list          | List whitelisted players", PluginName));
    }
}
