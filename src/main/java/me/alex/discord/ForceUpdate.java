package me.alex.discord;

import me.alex.ConfigurationValues;
import me.alex.SequenceBuilder;
import me.alex.sql.DatabaseConnectionManager;
import me.alex.sql.MessageUpdater;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * A command that can be executed by a user that has a certain role defined in conf.json that force updates the nerd role list
 * @see RoleUpdater
 * @see ConfigurationValues
 */
public class ForceUpdate extends ListenerAdapter implements RoleUpdater.Output {
    private final ConfigurationValues configurationValues;
    private final DatabaseConnectionManager databaseConnectionManager;
    private final MessageCooldownHandler messageCooldownHandler;
    private final RoleUpdater roleUpdater;
    private MessageReceivedEvent messageReceivedEvent;
    private MessageEmbed response = new EmbedBuilder().addField("Status", "No operations have been completed yet..", true).build();
    private MessageEmbed detailedResponse = new EmbedBuilder().addField("Status", "No operations have been completed yet..", true).build();

    /**
     * @param sequenceBuilder SequenceBuilder provides instances of the required classes to build the threads that can be used to create more instances
     * while still maintaining only one connection to the database at any one time.
     * @see DatabaseConnectionManager
     * @see me.alex.sql.DatabaseAccessListener
     * @see SequenceBuilder
     */
    public ForceUpdate(SequenceBuilder sequenceBuilder) {
        configurationValues = sequenceBuilder.getConfigurationValues();
        databaseConnectionManager = sequenceBuilder.getDatabaseConnectionManager();
        messageCooldownHandler = sequenceBuilder.getMessageCooldownHandler();
        roleUpdater = sequenceBuilder.getRoleUpdater();
    }

    /**
     * This method does one iteration of the cycle that the main class does.
     * @param e The MessageReceivedEvent object received when a user sends a message.
     * @see me.alex.Main
     * @see SequenceBuilder
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        messageReceivedEvent = e;
        if (!e.getMessage().getContentRaw().equalsIgnoreCase("!update")) return;
        if (e.getAuthor().isBot()) return;
        if (e.getMember() == null) return;
        if (e.getChannel().getType() == ChannelType.PRIVATE) {
            dmOutput(e);
            return;
        }
        List<Role> roles = e.getMember().getRoles();
        if (!(roles.stream().filter(i -> Arrays.asList(configurationValues.rolesAllowedToUpdate).contains(i.getIdLong())).count() < configurationValues.rolesAllowedToUpdate.length)) return;
        RoleUpdateQuery roleUpdateQuery = new RoleUpdateQuery(configurationValues, databaseConnectionManager);
        roleUpdater.addListener(this);
        roleUpdateQuery.addListener(roleUpdater);
        MessageUpdater messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
        messageUpdater.run();
    }

    /**
     * @param messageEmbed When there is output available it will send it.
     */
    @Override
    public void onEmbedOutputReady(MessageEmbed messageEmbed) {
        response = messageEmbed;
        if (messageReceivedEvent != null) {
            messageReceivedEvent.getChannel().sendMessage(response).queue();
        }
        messageReceivedEvent = null;
    }
    public void dmOutput(MessageReceivedEvent e) {
        if (!e.getMessage().getContentRaw().equalsIgnoreCase("!advinfo")){
            return;
        }
        e.getChannel().sendMessage(detailedResponse).queue();
    }

    @Override
    public void onAdvancedEmbedOutputReady(MessageEmbed messageEmbed) {
        detailedResponse = messageEmbed;
    }
}
