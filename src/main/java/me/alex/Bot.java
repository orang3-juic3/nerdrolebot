package me.alex;

import me.alex.discord.*;
import me.alex.sql.DatabaseManager;
import me.alex.sql.MessageUpdater;
import me.alex.sql.RetrieveLeaderboard;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;

/**
 * A class designed to making the whole building process easier. Registers listeners, passes around instances accordingly etc.
 * It also gets the main loop started.
 */
public class Bot {
    private final ConfigurationValues configurationValues = ConfigurationValues.getInstance();
    private JDA jda;
    private DatabaseManager databaseManager;
    private RoleUpdateQuery roleUpdateQuery;
    private MessageCooldownHandler messageCooldownHandler;
    private MessageUpdater messageUpdater;
    private RoleUpdater roleUpdater;
    private RetrieveLeaderboard retrieveLeaderboard;


    /**
     * Does the bot making.
     * <b>Do not use any of the getters or setters without calling this function!</b>
     */
    public Bot() {
        if (configurationValues == null) {
            // No error printing?
            System.exit(1);
        }
        EnumSet<GatewayIntent> gatewayIntents = EnumSet.allOf(GatewayIntent.class);
        JDABuilder jdaBuilder = JDABuilder.create(ConfigurationValues.getInstance().botToken, gatewayIntents);
        try {
            jda = jdaBuilder.build();
            jda.awaitReady();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
            return;
        }
        databaseManager = new DatabaseManager();
        roleUpdateQuery = new RoleUpdateQuery(databaseManager);
        roleUpdater = new RoleUpdater(jda, false);
        retrieveLeaderboard = new RetrieveLeaderboard();
        roleUpdateQuery.addListener(roleUpdater);
        jda.addEventListener(retrieveLeaderboard);
        roleUpdateQuery.addListener(retrieveLeaderboard);
        messageCooldownHandler = new MessageCooldownHandler();
        messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
        jda.addEventListener(messageCooldownHandler);
        jda.addEventListener(new ModPX());
        jda.addEventListener(new PurgeUdemy());
        ForceUpdate forceUpdate = new ForceUpdate(this);
        jda.addEventListener(forceUpdate);
        jda.addEventListener(new CarbonRestImpl());
    }

    /**
     * @return Returns the configurationValues instance.
     * @see ConfigurationValues
     */
    public ConfigurationValues getConfigurationValues() {
        return configurationValues;
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
    public JDA getJda() {
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
