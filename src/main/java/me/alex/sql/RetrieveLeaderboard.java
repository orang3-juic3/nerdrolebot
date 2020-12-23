package me.alex.sql;

import me.alex.ConfigurationValues;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetrieveLeaderboard implements DatabaseAccessListener {
    private boolean safeToAccess = true;
    private final ConfigurationValues configurationValues;
    private final DatabaseConnectionManager databaseConnectionManager;
    private final Map<MessageReceivedEvent, Integer> queueEvents = new HashMap<>();
    private final JDA jda;

    public RetrieveLeaderboard(ConfigurationValues configurationValues, DatabaseConnectionManager databaseConnectionManager, JDA jda) {
        this.configurationValues = configurationValues;
        this.databaseConnectionManager = databaseConnectionManager;
        this.jda = jda;
    }
    public void retrievePosition(MessageReceivedEvent e) {
        if (safeToAccess) {
            Connection conn = null;
            String url = "jdbc:sqlite:" + Paths.get("").toAbsolutePath().toString() + File.separator + "nerds.db";
            try {
                conn = DriverManager.getConnection(url);
                Statement statement = conn.createStatement();
                statement.executeUpdate("");
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException exc) {
                    exc.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDatabaseAccessEvent() {
        safeToAccess = false;
    }

    @Override
    public void onDatabaseStopAccessEvent() {

    }
}
