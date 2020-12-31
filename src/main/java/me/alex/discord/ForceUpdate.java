package me.alex.discord;

import me.alex.Bot;
import me.alex.ConfigurationValues;
import me.alex.sql.DatabaseManager;
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
    private final ConfigurationValues configurationValues = ConfigurationValues.getInstance();
    private final DatabaseManager databaseManager;
    private final MessageCooldownHandler messageCooldownHandler;
    private final RoleUpdater roleUpdater;
    private MessageEmbed response = new EmbedBuilder().addField("Status", "No operations have been completed yet..", true).build();
    private EmbedBuilder detailedResponse = new EmbedBuilder(response);
    private long timeOfUpdate;
    private boolean command;

    /**
     * @param bot SequenceBuilder provides instances of the required classes to build the threads that can be used to create more instances
     * while still maintaining only one connection to the database at any one time.
     * @see DatabaseManager
     * @see me.alex.sql.DatabaseAccessListener
     * @see Bot
     */
    public ForceUpdate(Bot bot) {
        databaseManager = bot.getDatabaseManager();
        messageCooldownHandler = bot.getMessageCooldownHandler();
        roleUpdater = new RoleUpdater(bot.getJda(), true);
    }

    /**
     * This method does one iteration of the cycle that the main class does.
     * @param e The MessageReceivedEvent object received when a user sends a message.
     * @see me.alex.Main
     * @see Bot
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!e.getMessage().getContentRaw().equalsIgnoreCase("!update") && !e.getMessage().getContentRaw().equalsIgnoreCase("!updateinfo")) return;
        if (e.getAuthor().isBot()) return;
        if (e.getChannel().getType() == ChannelType.PRIVATE) {
            if (e.getMessage().getContentRaw().equalsIgnoreCase("!update")) {
                return;
            }
            dmOutput(e);
            return;
        }
        if (e.getMessage().getContentRaw().equalsIgnoreCase("!updateinfo")) {
            e.getChannel().sendMessage("Please dm me this command!").queue();
            return;
        }
        if (e.getMember() == null) return; // should be after the above if because it will always be null if its a private channel..
        List<Role> roles = e.getMember().getRoles();
        boolean carryOn = false;
        for (Role role : roles) { // cleaned up this 'satanic' code at the request of Xemor. That 'satanic' code didn't actually work as well, thank you Xemor.
            if (Arrays.asList(configurationValues.rolesAllowedToUpdate).contains(role.getIdLong())) {
                carryOn = true;
                break;
            }
        }
        if(!carryOn) {
            return;
        }
        RoleUpdateQuery roleUpdateQuery = new RoleUpdateQuery(databaseManager,0);
        roleUpdater.addListener(this);
        roleUpdateQuery.addListener(roleUpdater);
        MessageUpdater messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
        messageUpdater.run();
        e.getChannel().sendMessage(response).queue();
        roleUpdater.removeListener(this);
    }

    /**
     * @param messageEmbed When there is output it will set the instance's embed to it.
     */
    @Override
    public void onEmbedOutputReady(MessageEmbed messageEmbed) {
        response = messageEmbed;
    }

    public void dmOutput(MessageReceivedEvent e) {
        long millis = System.currentTimeMillis() - timeOfUpdate;
        String timeDiff = RoleUpdater.getTimeFormatted(millis);
        if (command) {
            timeDiff += " | The source was a command executed by the user.";
        } else {
            timeDiff += " | The source was the bot updating the nerd list.";
        }
        detailedResponse.setFooter(timeDiff);
        if (!e.getMessage().getContentRaw().equalsIgnoreCase("!updateinfo")){
            return;
        }
        e.getChannel().sendMessage(detailedResponse.build()).queue();
    }

    @Override
    public void onAdvancedEmbedOutputReady(EmbedBuilder embedBuilder, long time, boolean command) {
        this.timeOfUpdate = time;
        this.command = command;
        detailedResponse = embedBuilder;
    }
}
