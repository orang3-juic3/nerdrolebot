package me.alex;

import me.alex.discord.CarbonRestImpl;
import me.alex.discord.ForceUpdate;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The main class of the project.
 */
// TODO: 20/12/2020 make all the listeners more generalised
public class Main {

    private List<Message> messages;

    /**
     * The main method of the program.
     * @param args Takes in launch arguments.
     */
    public static void main(String[] args) {
        try {
            SequenceBuilder sequenceBuilder = new SequenceBuilder();
            sequenceBuilder.build();
            sequenceBuilder.getJda().addEventListener(new ModPX());
            ForceUpdate forceUpdate = new ForceUpdate(sequenceBuilder);
            sequenceBuilder.getJda().addEventListener(forceUpdate);
            sequenceBuilder.getRoleUpdater().addListener(forceUpdate);
            sequenceBuilder.getJda().addEventListener(new CarbonRestImpl());
            firstTimeDatabaseSetup(sequenceBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called every time the program starts. It ensures the database exists and if not, makes a new one.
     * @throws IOException
     * @throws ClassNotFoundException
     * @param sequenceBuilder the sequence builder.
     * @see ConfigurationValues
     */
    synchronized public static void firstTimeDatabaseSetup(SequenceBuilder sequenceBuilder) throws IOException, ClassNotFoundException {
        JDA jda = sequenceBuilder.getJda();
        ConfigurationValues configurationValues = sequenceBuilder.getConfigurationValues();
        Class.forName("org.sqlite.JDBC");
        String workingDir = Paths.get("").toAbsolutePath().toString();
        if (new File(workingDir + File.separator + "nerds.db").exists()){
            startRunning(sequenceBuilder);
            return;
        }
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
                System.exit(1);
            }
        }
        Guild guild = jda.getGuildById(configurationValues.serverId);
        if (guild == null) {
            throw new InvalidConfigurationException("Invalid server id for id " + configurationValues.serverId + "!");
        }
        List<TextChannel> channels = guild.getTextChannels();
        User botId = jda.getSelfUser();
        final Member botMember = Objects.requireNonNull(jda.getGuildById(configurationValues.serverId)).getMember(botId);
        if (botMember == null) {
            throw new NullPointerException("Null bot!");
        }
        AtomicInteger masterCounter = new AtomicInteger(0);
        AtomicBoolean executeUpdates = new AtomicBoolean(false);
        List<String> sqlCalls = new CopyOnWriteArrayList<>();
        final long weekInMillis = (long) 6.048e+8;
        final long time = System.currentTimeMillis();
        final long compareTime = time - (weekInMillis * configurationValues.weeksOfData);
        for (TextChannel channel: channels) {
            if (Arrays.asList(configurationValues.ignoredChannels).contains(channel.getIdLong()) || !channel.canTalk(botMember)) {
                executeUpdates.set(masterCounter.getAndIncrement() == channels.size() - 1);
                continue;
            }
            MessageHistory messageHistory = MessageHistory.getHistoryBefore(channel, channel.getLatestMessageId()).limit(100).complete();
            List<Message> messages = messageHistory.getRetrievedHistory();
            while (!messages.isEmpty()) {
                Message placeholderMsg = messages.get(messages.size() - 1);
                for (Message message : messages) {
                    sqlCalls.add(String.format("INSERT INTO messages(id, time) VALUES (%s, %s)", message.getAuthor().getIdLong(), message.getTimeCreated().toEpochSecond() * 1000));
                }
                messageHistory = MessageHistory.getHistoryBefore(channel, placeholderMsg.getId()).limit(100).complete();
                messages = messageHistory.getRetrievedHistory();
                messages = messages.stream().filter(message -> message.getTimeCreated().toEpochSecond() * 1000 - compareTime >= 0).collect(Collectors.toList());
            }
            executeUpdates.set(masterCounter.getAndIncrement() == channels.size() - 1);
        }
        while (!executeUpdates.get()) {
            //wait because i dont know how to fucking do it better
        }
        executeInitialUpdates(sqlCalls);
        startRunning(sequenceBuilder);

        /* if (conn != null) {
            for (String i : sqlCalls) {
                Statement statement = conn.createStatement();
                statement.executeUpdate(i);
            }
        }
        System.out.println("Successfully connected to the database!"); */
    }
    public static void startRunning(SequenceBuilder sequenceBuilder) {
        while (true) {
            sequenceBuilder.getMessageUpdater().run();
        }
    }
    private static void executeInitialUpdates(List<String> sqlCalls) {
        String workingDir = Paths.get("").toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + workingDir + File.separator + "nerds.db";
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
}
