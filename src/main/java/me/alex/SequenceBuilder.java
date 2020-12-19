package me.alex;

import me.alex.discord.MessageCooldownHandler;
import me.alex.discord.RoleUpdater;
import me.alex.sql.DatabaseConnectionManager;
import me.alex.sql.MessageUpdater;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.EnumSet;

public class SequenceBuilder {
    private ConfigurationValues configurationValues;
    private JDA jda;
    private DatabaseConnectionManager databaseConnectionManager;
    private RoleUpdateQuery roleUpdateQuery;
    private MessageCooldownHandler messageCooldownHandler;
    private MessageUpdater messageUpdater;
    private RoleUpdater roleUpdater;
    public void build() throws Exception {
        configurationValues = ConfigurationValues.getInstance();
        if (configurationValues == null) {
            System.exit(1);
        }
        EnumSet<GatewayIntent> gatewayIntents = EnumSet.allOf(GatewayIntent.class);
        JDABuilder jdaBuilder = JDABuilder.create(ConfigurationValues.getInstance().botToken, gatewayIntents);
        jda = jdaBuilder.build();
        jda.awaitReady();
        databaseConnectionManager = new DatabaseConnectionManager();
        roleUpdateQuery = new RoleUpdateQuery(configurationValues, databaseConnectionManager);
        roleUpdater = new RoleUpdater(jda, configurationValues);
        roleUpdateQuery.addListener(roleUpdater);
        messageCooldownHandler = new MessageCooldownHandler(configurationValues, jda);
        messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
        jda.addEventListener(messageCooldownHandler);
    }

    public ConfigurationValues getConfigurationValues() {
        return configurationValues;
    }

    public void setConfigurationValues(ConfigurationValues configurationValues) {
        this.configurationValues = configurationValues;
    }

    public RoleUpdater getRoleUpdater() {
        return roleUpdater;
    }

    public void setRoleUpdater(RoleUpdater roleUpdater) {
        this.roleUpdater = roleUpdater;
    }

    public JDA getJda() {
        return jda;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public DatabaseConnectionManager getDatabaseConnectionManager() {
        return databaseConnectionManager;
    }

    public void setDatabaseConnectionManager(DatabaseConnectionManager databaseConnectionManager) {
        this.databaseConnectionManager = databaseConnectionManager;
    }

    public RoleUpdateQuery getRoleUpdateQuery() {
        return roleUpdateQuery;
    }

    public void setRoleUpdateQuery(RoleUpdateQuery roleUpdateQuery) {
        this.roleUpdateQuery = roleUpdateQuery;
    }

    public MessageCooldownHandler getMessageCooldownHandler() {
        return messageCooldownHandler;
    }

    public void setMessageCooldownHandler(MessageCooldownHandler messageCooldownHandler) {
        this.messageCooldownHandler = messageCooldownHandler;
    }

    public MessageUpdater getMessageUpdater() {
        return messageUpdater;
    }

    public void setMessageUpdater(MessageUpdater messageUpdater) {
        this.messageUpdater = messageUpdater;
    }
}
