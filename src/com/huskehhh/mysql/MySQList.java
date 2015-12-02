package com.huskehhh.mysql;

import com.huskehhh.mysql.mysql.MySQL;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQList extends JavaPlugin implements Listener {

    private static YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/MySQList/config.yml"));
    private static MySQL mysql = new MySQL(config.getString("MySQL.hostname"), config.getString("MySQL.port"), config.getString("MySQL.database"), config.getString("MySQL.username"), config.getString("MySQL.password"));

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        createConfig();
    }

    public void onDisable() {

    }

    private void createConfig() {
        boolean exists = new File("plugins/MySQList/config.yml").exists();

        if (!exists) {
            new File("plugins/MySQList").mkdir();
            config.options().header("MySQList, made by Husky!");
            config.set("MySQL.hostname", "localhost");
            config.set("MySQL.port", "3306");
            config.set("MySQL.database", "mysqlist");
            config.set("MySQL.username", "username");
            config.set("MySQL.password", "password");
            config.set("kickMessage", "&4You're not whitelisted on this server");
            try {
                config.save("plugins/MySQList/config.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (commandLabel.equalsIgnoreCase("sqlist") || commandLabel.equalsIgnoreCase("sl")) {

            if (args.length > 0) {
                String para = args[0];

                if (para.equalsIgnoreCase("add")) {
                    if (sender.hasPermission("sqlist.admin")) {
                        if (sender instanceof Player) {

                            Player p = (Player) sender;

                            String target = args[0];
                            Player targ = getServer().getPlayer(target);
                            String uuidOfTarget = targ.getUniqueId().toString();

                            try {
                                mysql.updateSQL("INSERT INTO `" + config.getString("MySQL.database") + "`.`whitelist` (`playername`, `uuid`) VALUES ('" + targ.getName() + ", '" + uuidOfTarget + "');");
                            } catch (SQLException e) {
                                e.printStackTrace();
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }

                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "[SQList] You do not have permission for this command.");
                    }
                } else if (para.equalsIgnoreCase("list")) {
                    try {
                        ResultSet select = mysql.querySQL("SELECT * FROM `whitelist`;");

                        select.next();

                        if (select.getString("playername") != null) {
                            sender.sendMessage(ChatColor.GREEN + "--------------------------");
                            sender.sendMessage(ChatColor.GOLD + "--- Players on whitelist ---");

                            while (select.next()) {
                                String name = select.getString("playername");
                                sender.sendMessage(ChatColor.DARK_GREEN + name);
                            }

                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }
        return false;
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String uuid = p.getUniqueId().toString();

        try {
            ResultSet nameCheck = mysql.querySQL("SELECT * FROM `whitelist` WHERE playername = '" + p.getName() + "'");
            ResultSet uuidCheck = mysql.querySQL("SELECT * FROM `whitelist` WHERE uuid = '" + uuid + "'");

            nameCheck.next();
            uuidCheck.next();


            if (uuidCheck.getString("uuid") == null) {
                if (!uuidCheck.getString("uuid").equals(uuid)) {
                    p.kickPlayer(ChatColor.translateAlternateColorCodes('&', config.getString("kickMessage")));
                }
            }


            if (nameCheck.getString("playername") == null) {
                if (!nameCheck.getString("playername").equals(p.getName())) {
                    // Check if uuid exists in there, change username accordingly
                    if (uuidCheck.getString("uuid") == null) {
                        if (uuidCheck.getString("uuid").equals(uuid)) {
                            mysql.updateSQL("DELETE FROM `whitelist` WHERE uuid = '" + uuid + "';");
                            mysql.updateSQL("INSERT INTO `" + config.getString("MySQL.database") + "`.`whitelist` (`playername`, `uuid`) VALUES ('" + p.getName() + ", '" + uuid + "');");
                        }
                    }

                }
            }

        } catch (SQLException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }
}
