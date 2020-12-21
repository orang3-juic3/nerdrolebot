package me.alex;

import me.alex.discord.ForceUpdate;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
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
        long time = System.currentTimeMillis();
        User botId = jda.getSelfUser();
        final Member botMember = Objects.requireNonNull(jda.getGuildById(configurationValues.serverId)).getMember(botId);
        if (botMember == null) {
            throw new NullPointerException("Null bot!");
        }
        AtomicInteger masterCounter = new AtomicInteger();
        AtomicBoolean executeUpdates = new AtomicBoolean(false);
        for (TextChannel i: channels) {
            if (Arrays.asList(configurationValues.ignoredChannels).contains(i.getIdLong()) || !i.canTalk(botMember)) {
                masterCounter.getAndIncrement();
                continue;
            }
            MessageHistory.getHistoryBefore(i, i.getLatestMessageId()).queue(j -> {
                List<String> sqlCalls = new ArrayList<>();
                if (j.isEmpty()) return;
                List<Message> messages = j.getRetrievedHistory();
                long weekInMillis = (long) 6.048e+8;
                messages = messages.stream().filter(Objects::nonNull)// remove nullular objectificationisations.
                        .filter(message -> !message.getAuthor().isBot())
                        .filter(
                                message -> (message.getTimeCreated().toEpochSecond() * 1000) >
                                        time - (weekInMillis * configurationValues.weeksOfData)).collect(Collectors.toList());
                HashMap<Long, Long> messagesSent = new HashMap<>(); //User ids and the epoch time of their most recent message sent.
                for (int k = 0; k < messages.size(); k++) {
                    messagesSent.putIfAbsent(messages.get(k).getAuthor().getIdLong(), messages.get(k).getTimeCreated().toEpochSecond() * 1000);
                    if (messagesSent.get(messages.get(k).getAuthor().getIdLong()) - messages.get(k).getTimeCreated().toEpochSecond() * 1000 > configurationValues.messageCooldown) {
                        messagesSent.put(messages.get(k).getAuthor().getIdLong(), messages.get(k).getTimeCreated().toEpochSecond() * 1000);
                        messages.set(k, null); // make it so that messages that were sent before the cooldown was over are NULLIFIED!!!
                    } else {
                        messagesSent.put(messages.get(k).getAuthor().getIdLong(), messages.get(k).getTimeCreated().toEpochSecond() * 1000);
                    }
                }
                messages = messages.stream().filter(Objects::nonNull).collect(Collectors.toList()); // remove the nulls that we just made:)
                for (Message m: messages) {
                    sqlCalls.add(String.format("INSERT INTO messages VALUES (%s, %s)", m.getAuthor().getIdLong(), m.getTimeCreated().toEpochSecond() * 1000));
                }
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
                masterCounter.getAndIncrement();
                if (masterCounter.get() == channels.size() - 1) {
                    executeUpdates.set(true);
                }
            }); // keep only messages that are less than 2 weeks old.
        }
        while (!executeUpdates.get()) {
            //wait because i dont know how to fucking do it better
        }
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
}
