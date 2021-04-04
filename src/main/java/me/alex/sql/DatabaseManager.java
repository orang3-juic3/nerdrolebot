package me.alex.sql;

import me.alex.Bot;
import me.alex.Config;
import me.alex.InvalidConfigurationException;
import me.alex.listeners.DatabaseAccessListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static me.alex.Main.startRunning;

/**
 * This is a class that helps deal with maintaining no concurrent connections to the database.
 * @see DatabaseAccessListener
 */
public class DatabaseManager {
    /**
     * This is a global point of access to the ExecutorService where all tasks
     * that are better off being done async
     * should be done.
     * @see ScheduledExecutorService
     */
    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final String workingDir = Paths.get("").toAbsolutePath().toString();
    private final String url = "jdbc:sqlite:" + workingDir + File.separator + "nerds.db";
    private final ArrayList<DatabaseAccessListener> databaseAccessListeners = new ArrayList<>();

    public static ScheduledExecutorService getService() {
        return service;
    }

    /**
     * @param databaseAccessListener Adds a listener that listens for when the database has been accessed, and when it has stopped being accessed.
     * @see DatabaseAccessListener
     */
    public void addListener(DatabaseAccessListener databaseAccessListener) {
        databaseAccessListeners.add(databaseAccessListener);
    }

    /**
     * Notifies listeners that the database is being accessed.
     * @see DatabaseAccessListener
     */
    synchronized public void notifyAccess() {
        for (DatabaseAccessListener i : databaseAccessListeners) {
            i.onDatabaseAccessEvent();
        }
    }

    /**
     * Notifies listeners that the database has stopped being accessed
     * @see DatabaseAccessListener
     */
    synchronized public void notifyStopAccess() {
        for (DatabaseAccessListener i : databaseAccessListeners) {
            i.onDatabaseStopAccessEvent();
        }
    }

    /**
     * This method is called every time the program starts. It ensures the database exists and if not, makes a new one.
     * @param bot the sequence builder.
     * @see Config
     */
    public void firstTimeDatabaseSetup(Bot bot) throws IOException, ClassNotFoundException {
        JDA jda = bot.getJDA();
        Config config = bot.getConfig();
        Class.forName("org.sqlite.JDBC");
        if (new File(workingDir + File.separator + "nerds.db").exists()){
            startRunning(bot);
            return;
        }
        System.err.println("Could not find existing nerds database, creating...");
        initializeTables();
        Guild guild = jda.getGuildById(config.serverId);
        if (guild == null) throw new InvalidConfigurationException("Invalid server id for id " + config.serverId + "!");
        List<TextChannel> channels = guild.getTextChannels();
        Member botMember = guild.getMember(jda.getSelfUser());
        if (botMember == null) throw new NullPointerException("Null bot!");
        AtomicInteger masterCounter = new AtomicInteger(0);
        List<String> sqlCalls = new ArrayList<>();
        final long time = System.currentTimeMillis();
        final long weekInMillis = (long) 6.048e+8;
        final long weeksAgo = time - (weekInMillis * config.weeksOfData);
        final HashMap<Long, Long> cooldownMap = new HashMap<>();
        for (TextChannel channel: channels) {
            if (Arrays.asList(config.ignoredChannels).contains(channel.getIdLong()) || !channel.canTalk(botMember)) {
                masterCounter.getAndIncrement();
                if (masterCounter.get() == channels.size()) {
                    executeSQLCalls(sqlCalls);
                    System.out.println("done!");
                    startRunning(bot);
                }
                continue;
            }
            List<Message> messages;
            String currentMessageID = channel.getLatestMessageId();
            do {
                MessageHistory messageHistory = MessageHistory.getHistoryBefore(channel, currentMessageID).limit(100).complete();
                messages = messageHistory.getRetrievedHistory();
                if (messages.isEmpty()) continue;
                messages = messages.stream().filter(message -> !message.getAuthor().isBot()).collect(Collectors.toList());
                currentMessageID = messages.get(messages.size() - 1).getId();
                messages = messages.stream().filter(message -> message.getTimeCreated().toEpochSecond() * 1000 - weeksAgo >= 0).collect(Collectors.toList());
                for (Message message : messages) {
                    Long timeMade = message.getTimeCreated().toEpochSecond() * 1000;
                    Long lastTime = cooldownMap.get(message.getAuthor().getIdLong());
                    if (lastTime == null) { // im such a nink. were iterating backwards in terms of time
                        cooldownMap.put(message.getAuthor().getIdLong(), timeMade);
                        sqlCalls.add(String.format("INSERT INTO messages(id, time) VALUES (%s, %s)", message.getAuthor().getId(), message.getTimeCreated().toEpochSecond() * 1000));
                    } else if (lastTime - timeMade >= config.messageCooldown) {
                        cooldownMap.put(message.getAuthor().getIdLong(), timeMade);
                        sqlCalls.add(String.format("INSERT INTO messages(id, time) VALUES (%s, %s)", message.getAuthor().getId(), message.getTimeCreated().toEpochSecond() * 1000));
                    }
                }
            }
            while (!messages.isEmpty());
            masterCounter.getAndIncrement();
            if (masterCounter.get() == channels.size()) {
                executeSQLCalls(sqlCalls);
                System.out.println("done!");
                startRunning(bot);
            }
        }
    }
    private void executeSQLCalls(List<String> sqlCalls) {
        Connection connection = null; // we need to make it null because DriverManager#getConnection() doesn't throw a hissy fit if something is wrong.
        try {
            connection = DriverManager.getConnection(url);
            if (connection != null) {
                for (String s : sqlCalls) {
                    Statement statement = connection.createStatement();
                    statement.executeUpdate(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            }catch (SQLException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }

    public void initializeTables() throws IOException {
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
                System.exit(1);
            }
        }
    }
}
