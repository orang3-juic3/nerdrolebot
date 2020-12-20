package me.alex.sql;


import me.alex.ConfigurationValues;

import java.io.File;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class deals with querying the SQL database for messages sent.
 */
public class RoleUpdateQuery implements Runnable, DatabaseAccessListener {
    private final ConfigurationValues configurationValues;
    private boolean inQueue = false;
    private boolean safeToAccess = true;
    private boolean execute = true;
    private final ArrayList<ScoreMapReadyListener> scoreMapReadyListeners = new ArrayList<>();
    private final DatabaseConnectionManager databaseConnectionManager;

    /**
     * @param scoreMapReadyListener Adds a ScoreMapReadyListener instance to the listener ArrayList.
     * @see me.alex.discord.RoleUpdater
     * @see ScoreMapReadyListener
     */
    public void addListener(ScoreMapReadyListener scoreMapReadyListener) {
        scoreMapReadyListeners.add(scoreMapReadyListener);
    }

    /**
     * The constructor for this class
     * @param configurationValues The instance of the ConfigurationValues, needed to set various things across the code.
     * @param databaseConnectionManager The instance of the DatabaseConnectionManager, which ensures that there are no concurrent connections to the database.
     * @see ConfigurationValues
     * @see DatabaseConnectionManager
     * @see DatabaseAccessListener
     */
    public RoleUpdateQuery(ConfigurationValues configurationValues, DatabaseConnectionManager databaseConnectionManager) {
        this.configurationValues = configurationValues;
        databaseConnectionManager.addListener(this);
        this.databaseConnectionManager = databaseConnectionManager;
    }

    /**
     * The run method for the thread that is created by MessageUpdater for this class. It checks whether it is safe to access the database, then queries it.
     * If it isn't safe to access the database, the process is put in a queue, and gets executed when it is safe to access the database again.
     * @see MessageUpdater
     * @see DatabaseConnectionManager
     * @see DatabaseAccessListener
     */
    @Override
    public void run() {
        if (safeToAccess) {
            inQueue = false;
            databaseConnectionManager.notifyAccess();
            setScoreMap();
            databaseConnectionManager.notifyStopAccess();
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            inQueue = true;
        }
    }

    /**
     * When the database is accessed, this makes sure that the thread will not attempt to access the database.
     * @see DatabaseConnectionManager
     * @see DatabaseAccessListener
     */
    @Override
    synchronized public void onDatabaseAccessEvent() {
        safeToAccess = false;
    }

    /**
     * When the database has stopped being accessed, the process gets executed if it is in a queue.
     * @see DatabaseConnectionManager
     * @see DatabaseAccessListener
     */
    @Override
    public void onDatabaseStopAccessEvent()  {
        if (inQueue) {
            databaseConnectionManager.notifyAccess();
            setScoreMap();
            databaseConnectionManager.notifyStopAccess();
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        safeToAccess = true;

    }

    /**
     * This method makes a HashMap of User IDs and how many messages they've sent in the configured time.
     * Then it notifies listeners that a score map is ready.
     * @see ConfigurationValues
     * @see ScoreMapReadyListener
     * @see me.alex.discord.RoleUpdater
     * @see net.dv8tion.jda.api.entities.User
     */
    private void setScoreMap() {
        HashMap<Long, Long> scoreMap = new HashMap<>();
        String workingDir = Paths.get("").toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + workingDir + File.separator + "nerds.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                long weekInMillis = (long) 6.048e+8;
                String sql = "SELECT DISTINCT id FROM messages WHERE time >= " + System.currentTimeMillis() + " - " + (weekInMillis * configurationValues.weeksOfData);
                Statement statement = conn.createStatement();
                statement.execute(sql);
                ResultSet resultSet = statement.getResultSet();
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    if (!Arrays.asList(configurationValues.exemptionList).contains(id)) scoreMap.put(id, null);
                }
                for (long i : scoreMap.keySet()) {
                    sql = String.format("SELECT count(id) FROM messages WHERE time >= %s - %s and id = %s", System.currentTimeMillis(), (weekInMillis * configurationValues.weeksOfData), i);
                    statement = conn.createStatement();
                    statement.execute(sql);
                    resultSet = statement.getResultSet();
                    while (resultSet.next()) {
                        scoreMap.put(i, resultSet.getLong("count(id)"));
                    }
                }
                for (ScoreMapReadyListener scoreMapReadyListener : scoreMapReadyListeners) {
                    scoreMapReadyListener.onScoreMapReadyEvent(scoreMap);
                }
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

    /**
     * @return Returns the instance of DatabaseConnectionManager associated with this class.
     * @see DatabaseConnectionManager
     */
    public DatabaseConnectionManager getDatabaseConnectionManager() {
        return databaseConnectionManager;
    }
}
