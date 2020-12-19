package me.alex.sql;

import me.alex.discord.MessageCooldownHandler;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MessageUpdater implements DatabaseAccessListener, Runnable {
    private final MessageCooldownHandler messageCooldownHandler;
    private final RoleUpdateQuery roleUpdateQuery;
    private boolean safeToAccess = true;
    private boolean inQueue = false;

    public MessageUpdater(RoleUpdateQuery roleUpdateQuery, MessageCooldownHandler messageCooldownHandler) {
        this.messageCooldownHandler = messageCooldownHandler;
        this.roleUpdateQuery = roleUpdateQuery;
    }

    @Override
    public void run() {
        if (safeToAccess) {
            roleUpdateQuery.getDatabaseConnectionManager().notifyAccess();
            updateMessageTable();
            roleUpdateQuery.getDatabaseConnectionManager().notifyStopAccess();
            roleUpdateQuery.run();
        } else {
            inQueue = true;
        }
    }

    @Override
    public void onDatabaseAccessEvent() {
        safeToAccess = false;
    }

    @Override
    public void onDatabaseStopAccessEvent() {
        if (inQueue) {
            roleUpdateQuery.getDatabaseConnectionManager().notifyAccess();
            updateMessageTable();
            roleUpdateQuery.getDatabaseConnectionManager().notifyStopAccess();
            roleUpdateQuery.run();
        }
    }
    private void updateMessageTable() {
        ArrayList<String> sqlQueries = messageCooldownHandler.generateSqlCalls();
        String workingDir = Paths.get("").toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + workingDir + File.separator + "nerds.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            for (String i: sqlQueries) {
                Statement statement = conn.createStatement();
                statement.executeUpdate(i);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
