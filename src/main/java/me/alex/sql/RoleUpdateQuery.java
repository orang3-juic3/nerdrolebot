package me.alex.sql;


import me.alex.meta.Config;
import me.alex.listeners.DatabaseAccessListener;
import me.alex.listeners.ScoreMapReadyListener;

import java.io.File;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class deals with querying the SQL database for messages sent.
 */
public class RoleUpdateQuery implements Runnable, DatabaseAccessListener {
    private final Config config = Config.getInstance();
    private boolean inQueue = false;
    private boolean safeToAccess = true;
    private final ArrayList<ScoreMapReadyListener> scoreMapReadyListeners = new ArrayList<>();
    private final DatabaseManager databaseManager;

    /**
     * @param scoreMapReadyListener Adds a ScoreMapReadyListener instance to the listener ArrayList.
     * @see me.alex.discord.RoleUpdater
     * @see ScoreMapReadyListener
     */
    public void addListener(ScoreMapReadyListener scoreMapReadyListener) {
        scoreMapReadyListeners.add(scoreMapReadyListener);
    }

    /**
     * The preferred constructor for this class, where the delay is the one defined in Config
     * @param databaseManager The instance of the DatabaseConnectionManager, which ensures that there are no concurrent connections to the database.
     * @see Config
     * @see DatabaseManager
     * @see DatabaseAccessListener
     */
    public RoleUpdateQuery(DatabaseManager databaseManager) {
        databaseManager.addListener(this);
        this.databaseManager = databaseManager;
    }


    /**
     * The run method for the thread that is created by MessageUpdater for this class. It checks whether it is safe to access the database, then queries it.
     * If it isn't safe to access the database, the process is put in a queue, and gets executed when it is safe to access the database again.
     * @see MessageUpdater
     * @see DatabaseManager
     * @see DatabaseAccessListener
     */
    @Override
    public void run() {
        if (safeToAccess) {
            inQueue = false;
            databaseManager.notifyAccess();
            setScoreMap();
            databaseManager.notifyStopAccess();
            /*try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
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
    synchronized public void onDatabaseAccessEvent() {
        safeToAccess = false;
    }

    /**
     * When the database has stopped being accessed, the process gets executed if it is in a queue.
     * @see DatabaseManager
     * @see DatabaseAccessListener
     */
    @Override
    public void onDatabaseStopAccessEvent()  {
        if (inQueue) {
            databaseManager.notifyAccess();
            setScoreMap();
            databaseManager.notifyStopAccess();
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
     * @see Config
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
                String sql = "SELECT DISTINCT id FROM messages WHERE time >= " + System.currentTimeMillis() + " - " + (weekInMillis * config.getWeeksOfData());
                Statement statement = conn.createStatement();
                statement.execute(sql);
                ResultSet resultSet = statement.getResultSet();
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    scoreMap.put(id, null);
                }
                HashMap<Long, Long> allScoreMap = new HashMap<>(scoreMap);
                for (long i : scoreMap.keySet()) {
                    sql = String.format("SELECT count(id) FROM messages WHERE time >= %s - %s and id = %s", System.currentTimeMillis(), (weekInMillis * config.getWeeksOfData()), i);
                    statement = conn.createStatement();
                    statement.execute(sql);
                    resultSet = statement.getResultSet();
                    while (resultSet.next()) {
                        scoreMap.put(i, resultSet.getLong("count(id)"));
                    }
                }
                for (long i : allScoreMap.keySet()) {
                    sql = String.format("SELECT count(id) FROM messages WHERE id = %s", i);
                    statement = conn.createStatement();
                    statement.execute(sql);
                    resultSet = statement.getResultSet();
                    while (resultSet.next()) {
                        allScoreMap.put(i, resultSet.getLong("count(id)"));
                    }
                }
                for (ScoreMapReadyListener scoreMapReadyListener : scoreMapReadyListeners) {
                    scoreMapReadyListener.onScoreMapReadyEvent(scoreMap);
                    scoreMapReadyListener.onFullScoreMapReadyEvent(allScoreMap);
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
     * @see DatabaseManager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
