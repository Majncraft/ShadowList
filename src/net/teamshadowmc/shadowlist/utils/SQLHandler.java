package net.teamshadowmc.shadowlist.utils;

import lombok.NonNull;
import net.teamshadowmc.shadowlist.ShadowList;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SQLHandler {

    public static SQLHandler createMySQL(String host, int port, String user, String password, String database) {
        return new MySQL(host, port, user, password, database);
    }
    private String SQLTable = ShadowList.SQLTable;
    private String PluginName = ShadowList.PluginName;

    public abstract void execute(@NonNull String query) throws Exception;

    public abstract void query(@NonNull String query, @NonNull Callback<ResultSet> result) throws Exception;

    public void createTables() throws Exception {
        execute("CREATE TABLE IF NOT EXISTS `"+SQLTable+"` (" +
                "`id` bigint(20) unsigned NOT NULL AUTO_INCREMENT," +
                "`uuid` varchar(36) DEFAULT NULL," +
                "`name` varchar(36) NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `id` (`id`)" +
                ") ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;");
    }

    public void addToWhitelist(final CommandSender reply, String username) throws Exception {
        try {
            execute(String.format("INSERT IGNORE INTO `%s` (`name`) VALUES ('%s');", SQLTable, username));
            reply.sendMessage(String.format("%s added to whitelist", username));
        } catch (SQLException ex) {
            reply.sendMessage(String.format("Error adding %s to whitelist. See console for details.", username));
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            reply.sendMessage(String.format("Error adding %s to whitelist. See log for details.", username));
            ex.printStackTrace();
        }
    }

    public void removeFromWhitelist(final CommandSender reply, final String username) throws Exception {
        try {
            query(String.format("SELECT * FROM `%s` WHERE name = '%s'", SQLTable, username), checkResult -> {
                if (checkResult.isBeforeFirst()) {
                    checkResult.next();
                    if (checkResult.getString("name") != null) {
                        execute(String.format("DELETE FROM `%s` WHERE name = '%s'", SQLTable, username));
                        reply.sendMessage(String.format("[%s] Removed %s from %s.", PluginName, SQLTable, username));
                    }
                } else {
                    reply.sendMessage(String.format("[%s] Failed to remove %s. Are you sure they are whitelisted and you spelled it right?", PluginName, username));
                }
            });
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            reply.sendMessage(String.format("[%s] You are missing a required class file! See above stacktrace for more info.", PluginName));
        }
    }
    public void checkWhitelist(final CommandSender reply, final String username) throws Exception {
        try {
            query(String.format("SELECT * FROM `%s` WHERE name = '%s'", SQLTable, username), checkResult -> {
                if (checkResult.isBeforeFirst()) {
                    checkResult.next();
                    if (checkResult.getString("name") != null) {
                        reply.sendMessage(String.format("[%s] %s is on the whitelist.", PluginName, username));
                    } else {
                        reply.sendMessage(String.format("[%s] %s is not on the whitelist.", PluginName, username));
                    }
                } else {
                    reply.sendMessage(String.format("[%s] %s is not on the whitelist.", PluginName, username));
                }
            });
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            reply.sendMessage(String.format("[%s] You are missing a required class file! See above stacktrace for more info.", PluginName));
        }
    }

    public String preLoginCheck(final String joiningPlayer, final String joiningUUID) throws SQLException {
        final String[] canJoin = {null};
        try {
            query(String.format("SELECT uuid, name FROM `%s` WHERE (name = '%s' OR uuid = '%s')", SQLTable, joiningPlayer, joiningUUID), (ResultSet checkResult) -> {
                if (checkResult.isBeforeFirst()) {

                    checkResult.next();
                    if (checkResult.getString("uuid") != null) {
                        if (joiningUUID.equalsIgnoreCase(checkResult.getString("uuid"))) {
                            if (!joiningPlayer.equalsIgnoreCase(checkResult.getString("name"))) {
                                try {
                                    execute(String.format("UPDATE `%s` SET `name` = '%s' WHERE `uuid` = '%s'", SQLTable, joiningPlayer, joiningUUID));
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                } catch (ClassNotFoundException ex) {
                                    return;
                                }
                            }
                        }
                    } else if (checkResult.getString("name").equalsIgnoreCase(joiningPlayer)) {
                        if (checkResult.getString("uuid") == null) {
                            try {
                                execute(String.format("UPDATE `%s` SET `uuid` = '%s' WHERE `name` = '%s'", SQLTable, joiningUUID, joiningPlayer));
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            } catch (ClassNotFoundException ex) {
                                return;
                            }
                        }
                    } else {
                        canJoin[0] = "no";
                    }
                } else {
                    canJoin[0] = "no";
                }
            });
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.println("[!!!#1] PLUGIN ERROR, KICK PLAYER");
            canJoin[0] = "error";
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            System.err.println("[!!!#2] PLUGIN ERROR, KICK PLAYER");
            canJoin[0] = "error";
        } catch (Exception e) {
            System.err.println("[!!!#3] PLUGIN ERROR, KICK PLAYER");
            e.printStackTrace();
        }
        return canJoin[0];

    }

    /*public void close() throws SQLException {
        this.close();
    }*/
}
