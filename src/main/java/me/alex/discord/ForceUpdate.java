package me.alex.discord;

import me.alex.ConfigurationValues;
import me.alex.SequenceBuilder;
import me.alex.sql.DatabaseConnectionManager;
import me.alex.sql.MessageUpdater;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ForceUpdate extends ListenerAdapter {
    private final ConfigurationValues configurationValues;
    private final DatabaseConnectionManager databaseConnectionManager;
    private final MessageCooldownHandler messageCooldownHandler;
    private final RoleUpdater roleUpdater;

    public ForceUpdate(SequenceBuilder sequenceBuilder) {
        configurationValues = sequenceBuilder.getConfigurationValues();
        databaseConnectionManager = sequenceBuilder.getDatabaseConnectionManager();
        messageCooldownHandler = sequenceBuilder.getMessageCooldownHandler();
        roleUpdater = sequenceBuilder.getRoleUpdater();
    }
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!e.getMessage().getContentRaw().equalsIgnoreCase("!update")) return;
        if (e.getAuthor().isBot()) return;
        if (e.getMember() == null) return;
        List<Role> roles = e.getMember().getRoles();
        if (!(roles.stream().filter(i -> Arrays.asList(configurationValues.rolesAllowedToUpdate).contains(i.getIdLong())).count() < configurationValues.rolesAllowedToUpdate.length)) return;
        RoleUpdateQuery roleUpdateQuery = new RoleUpdateQuery(configurationValues, databaseConnectionManager);
        roleUpdateQuery.addListener(roleUpdater);
        MessageUpdater messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
        messageUpdater.run();
    }
}
