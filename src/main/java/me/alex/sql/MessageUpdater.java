package me.alex.sql;

import me.alex.discord.MessageCooldownHandler;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * This class deals with logging messages sent by users.
 */
public class MessageUpdater implements DatabaseAccessListener, Runnable {
    private final MessageCooldownHandler messageCooldownHandler;
    private final RoleUpdateQuery roleUpdateQuery;
    private boolean safeToAccess = true;
    private boolean inQueue = false;

    /**
     * @param roleUpdateQuery An instance of RoleUpdateQuery to run after updating messages.
     * @param messageCooldownHandler An instance of MessageCooldownHandler to get the users to update.
     */
    public MessageUpdater(RoleUpdateQuery roleUpdateQuery, MessageCooldownHandler messageCooldownHandler) {
        this.messageCooldownHandler = messageCooldownHandler;
        this.roleUpdateQuery = roleUpdateQuery;
    }

    /**
     * The main method this thread runs. If it is safe to access the database, it will update the database, then start a thread of a RoleUpdateQuery instance.
     * @see DatabaseAccessListener
     * @see DatabaseManager
     * @see RoleUpdateQuery
     */
    @Override
    public void run() {
        if (safeToAccess) {
            roleUpdateQuery.getDatabaseManager().notifyAccess();
            updateMessageTable();
            roleUpdateQuery.getDatabaseManager().notifyStopAccess();
            roleUpdateQuery.run();
        } else {
            inQueue = true;
        }
    }

    /**
     * When the database is accessed, this makes sure that the thread will not attempt to access the database.
     * @see DatabaseManager
     * @see DatabaseAccessListener
     */
    @Override
    public void onDatabaseAccessEvent() {
        safeToAccess = false;
    }
    /**
     * When the database has stopped being accessed, the process gets executed if it is in a queue.
     * @see DatabaseManager
     * @see DatabaseAccessListener
     */
    @Override
    public void onDatabaseStopAccessEvent() {
        if (inQueue) {
            roleUpdateQuery.getDatabaseManager().notifyAccess();
            updateMessageTable();
            roleUpdateQuery.getDatabaseManager().notifyStopAccess();
            roleUpdateQuery.run();
        }
    }

    /**
     * This will update the database with logged messages by a MessageCooldownHandler instance
     * @see MessageCooldownHandler
     */
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
