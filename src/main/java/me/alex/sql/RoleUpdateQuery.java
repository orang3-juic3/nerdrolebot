package me.alex.sql;


import me.alex.ConfigurationValues;
import me.alex.InputThread;

import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class RoleUpdateQuery implements Runnable, DatabaseAccessListener, InputThread.Close {
    private final ConfigurationValues configurationValues;
    private boolean inQueue = false;
    private boolean safeToAccess = true;
    private boolean execute = true;
    private final ArrayList<ScoreMapReadyListener> scoreMapReadyListeners = new ArrayList<>();
    private final DatabaseConnectionManager databaseConnectionManager;

    public void addListener(ScoreMapReadyListener scoreMapReadyListener) {
        scoreMapReadyListeners.add(scoreMapReadyListener);
    }
    public RoleUpdateQuery(ConfigurationValues configurationValues, DatabaseConnectionManager databaseConnectionManager) {
        this.configurationValues = configurationValues;
        databaseConnectionManager.addListener(this);
        this.databaseConnectionManager = databaseConnectionManager;
    }

    @Override
    public void run() {
        while (execute) {
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
    }

    @Override
    synchronized public void onDatabaseAccessEvent() {
        safeToAccess = false;
    }

    @Override
    synchronized public void onDatabaseStopAccessEvent()  {
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
    private void setScoreMap() {
        HashMap<Long, Long> scoreMap = new HashMap<>();
        String workingDir = Paths.get("").toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + workingDir + "\\nerds.db";
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

    @Override
    public void stopProgram() {
        execute = false;
    }
}
