package me.alex;

import me.alex.discord.ForceUpdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
/**
 * Hello world!
 *
 */
public class Main {

    public static void main(String[] args) {
        try {
            firstTimeDatabaseSetup();
            SequenceBuilder sequenceBuilder = new SequenceBuilder();
            sequenceBuilder.build();
            sequenceBuilder.getJda().addEventListener(new ForceUpdate(sequenceBuilder));
            while (true) {
                sequenceBuilder.getMessageUpdater().run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void firstTimeDatabaseSetup() throws IOException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        String workingDir = Paths.get("").toAbsolutePath().toString();
        if (new File(workingDir + File.separator + "nerds.db").exists()) return;
        System.err.println("Could not find existing nerds database, creating...");
        String url = "jdbc:sqlite:" + workingDir + File.separator + "nerds.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("Nerds database file has been created!");
                String sql = "CREATE TABLE messages (id integer, time sqlite3_int64);";
                Statement statement = conn.createStatement();
                statement.execute(sql);
                System.out.println("Created table messages!");
                sql = "CREATE TABLE levels (id integer, score integer);";
                statement = conn.createStatement();
                statement.execute(sql);
                System.out.println("Created table levels!");
            }
        } catch (SQLException e) {
            throw new IOException("Couldn't create new database with tables!", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Successfully connected to the database!");
    }
}
