package net.teamshadowmc.shadowlist.utils;

import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import lombok.Synchronized;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MySQL extends SQLHandler {

    private final Object locker = new Object();
    private final HikariDataSource dataSource;

    public MySQL(String host, int port, String user, String password, String database) {
        dataSource = new HikariDataSource();
        dataSource.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        dataSource.setPoolName("Majncraft | Whitelist");
        dataSource.setMaxLifetime(60000);
        dataSource.setIdleTimeout(45000);
        dataSource.setMaximumPoolSize(20);
        dataSource.addDataSourceProperty("prepStmtCacheSize", 250);
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        dataSource.addDataSourceProperty("cachePrepStmts", true);
        dataSource.addDataSourceProperty("serverName", host);
        dataSource.addDataSourceProperty("port", port);
        dataSource.addDataSourceProperty("databaseName", database);
        dataSource.addDataSourceProperty("user", user);
        dataSource.addDataSourceProperty("password", password);
    }


    @Override
    @Synchronized("locker")
    public void execute(@NonNull String query) throws Exception {
        PreparedStatement statement = dataSource.getConnection().prepareStatement(query);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    @Synchronized("locker")
    public void query(@NonNull String query, @NonNull Callback<ResultSet> result) throws Exception {
        PreparedStatement statement = dataSource.getConnection().prepareStatement(query);
        result.result(statement.executeQuery());
        statement.close();
    }
}
