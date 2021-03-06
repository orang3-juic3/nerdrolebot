package me.alex.meta;

import me.alex.discord.*;
import me.alex.sql.DatabaseManager;
import me.alex.sql.MessageUpdater;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class designed to making the whole building process easier. Registers listeners, passes around instances accordingly etc.
 * It also gets the main loop started.
 */
public class Bot {
    private Config config = Config.getInstance();
    private static JDA jda;
    private DatabaseManager databaseManager;
    private RoleUpdateQuery roleUpdateQuery;
    private MessageCooldownHandler messageCooldownHandler;
    private MessageUpdater messageUpdater;
    private RoleUpdater roleUpdater;
    private final RetrieveLeaderboard retrieveLeaderboard;


    /**
     * Does the bot making.
     * <b>Do not use any of the getters or setters without calling this function!</b>
     */
    public Bot() {
        EnumSet<GatewayIntent> gatewayIntents = EnumSet.allOf(GatewayIntent.class);
        JDABuilder jdaBuilder = JDABuilder.create(Config.getInstance().getBotToken(), gatewayIntents).setEventManager(new AnnotatedEventManager());
        databaseManager = new DatabaseManager();
        roleUpdateQuery = new RoleUpdateQuery(databaseManager);
        roleUpdater = new RoleUpdater(false);
        messageCooldownHandler = new MessageCooldownHandler();
        ForceUpdate forceUpdate = new ForceUpdate(this);
        roleUpdater.addListener(forceUpdate);
        retrieveLeaderboard = new RetrieveLeaderboard();
        roleUpdateQuery.addListener(roleUpdater);
        jdaBuilder.addEventListeners(retrieveLeaderboard);
        roleUpdateQuery.addListener(retrieveLeaderboard);
        messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
        jdaBuilder.addEventListeners(messageCooldownHandler, new ModPX(),
                new Blacklist(),
                forceUpdate,
                new CarbonRestImpl(),
                configReloadListener());
        try {
            jda = jdaBuilder.build();
            jda.awaitReady();
            jda.openPrivateChannelById(479285497487949853L).queue(it -> it.sendMessage("Test").queue());
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            System.setErr(new PrintStream(b));
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!b.toString().equals("")) {
                        jda.openPrivateChannelById(479285497487949853L).queue(it -> it.sendMessage(b.toString()).queue());
                        b.reset();
                    }
                }
            },0 , 1000);
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }

    }
    private Object configReloadListener() {
        return new Object() {
            @SubscribeEvent
            public void onGuildMessage(GuildMessageReceivedEvent e) {
                new Thread(() -> {
                    if (e.getGuild().getIdLong() != Config.getInstance().getServerId()) return;
                    if (e.getAuthor().isBot() || e.isWebhookMessage()) return;
                    if (!e.getMessage().getContentDisplay().equalsIgnoreCase(config.getPrefix() + "reloadconfig")) return;
                    if (e.getMember() != null) {
                        if (e.getMember().getRoles().stream().mapToLong(Role::getIdLong).anyMatch(it -> it == 772163720062173214L)) {                        // mega nerd id should b in config...
                            Config.loadConfig();
                            config = Config.getInstance();
                            e.getChannel().sendMessage("" + config.getPrefix()).queue();
                        }
                    }
                }).start();
            }
        };
    }

    /**
     * @return Returns the config instance.
     * @see Config
     */
    public Config getConfig() {
        return config;
    }


    /**
     * @return Returns the RoleUpdater instance associated with this method.
     * @see RoleUpdater
     */
    public RoleUpdater getRoleUpdater() {
        return roleUpdater;
    }

    /**
     * @param roleUpdater Sets this classes roleUpdater instance to the instance from the parameter.
     * @see RoleUpdater
     */
    public void setRoleUpdater(RoleUpdater roleUpdater) {
        this.roleUpdater = roleUpdater;
    }

    /**
     * @return The JDA instance in which all discord api calls are done from.
     * @see JDA
     */
    public static JDA getJDA() {
        return jda;
    }

    /**
     * @return An instance of the databaseConnectionManager or null if you haven't called SequenceBuilder#build(). This will have listeners inside.
     * @see Bot
     * @see DatabaseManager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * @param databaseManager Sets the instance of DatabaseConnectionManager within this class
     * @see DatabaseManager
     */
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * @return Returns an instance of RoleUpdateQuery with the command boolean set to false or null if you haven't called SequenceBuilder#build().
     * @see RoleUpdateQuery
     */
    public RoleUpdateQuery getRoleUpdateQuery() {
        return roleUpdateQuery;
    }

    /**
     * @param roleUpdateQuery Sets the instance of RoleUpdateQuery within this instance of SequenceBuilder.
     * @see RoleUpdateQuery
     */
    public void setRoleUpdateQuery(RoleUpdateQuery roleUpdateQuery) {
        this.roleUpdateQuery = roleUpdateQuery;
    }

    /**
     * @return Returns an instance of MessageCooldownHandler or null if you haven't called SequenceBuilder#build().
     * @see MessageCooldownHandler
     */
    public MessageCooldownHandler getMessageCooldownHandler() {
        return messageCooldownHandler;
    }

    /**
     * @param messageCooldownHandler Sets the instance of MessageCooldownHandler within this instance of SequenceBuilder.
     * @see MessageCooldownHandler
     */
    public void setMessageCooldownHandler(MessageCooldownHandler messageCooldownHandler) {
        this.messageCooldownHandler = messageCooldownHandler;
    }

    /**
     * @return Returns an instance of MessageUpdater or null if you haven't called SequenceBuilder#build()
     */
    public MessageUpdater getMessageUpdater() {
        return messageUpdater;
    }

    /**
     * @param messageUpdater Sets the instance of MessageUpdater within this instance of SequenceBuilder.
     * @see MessageUpdater
     */
    public void setMessageUpdater(MessageUpdater messageUpdater) {
        this.messageUpdater = messageUpdater;
    }

    public RetrieveLeaderboard getRetrieveLeaderboard() {
        return retrieveLeaderboard;
    }
}
