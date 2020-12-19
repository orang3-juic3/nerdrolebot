package me.alex.sql;

import me.alex.InputThread;
import me.alex.discord.MessageCooldownHandler;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MessageUpdater implements DatabaseAccessListener, Runnable, InputThread.Close {
    private final DatabaseConnectionManager databaseConnectionManager;
    private final MessageCooldownHandler messageCooldownHandler;
    private boolean safeToAccess = true;
    private boolean inQueue = false;
    private boolean execute = true;

    public MessageUpdater(DatabaseConnectionManager databaseConnectionManager, MessageCooldownHandler messageCooldownHandler) {
        this.databaseConnectionManager = databaseConnectionManager;
        this.messageCooldownHandler = messageCooldownHandler;
    }

    @Override
    public void run() {
        if (safeToAccess) {
            databaseConnectionManager.notifyAccess();
            updateMessageTable();
            databaseConnectionManager.notifyStopAccess();
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
            databaseConnectionManager.notifyAccess();
            updateMessageTable();
            databaseConnectionManager.notifyStopAccess();
            try {
                Thread.sleep(115000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void updateMessageTable() {
        ArrayList<String> sqlQueries = messageCooldownHandler.generateSqlCalls();
        String workingDir = Paths.get("").toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + workingDir + "/nerds.db";
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

    @Override
    public void stopProgram() {
        execute = false;
    }
}
