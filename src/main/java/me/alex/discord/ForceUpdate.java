package me.alex.discord;

import me.alex.meta.Bot;
import me.alex.meta.Config;
import me.alex.listeners.DatabaseAccessListener;
import me.alex.meta.Main;
import me.alex.sql.DatabaseManager;
import me.alex.sql.MessageUpdater;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A command that can be executed by a user that has a certain role defined in conf.json that force updates the nerd role list
 * @see RoleUpdater
 * @see Config
 */
public class ForceUpdate implements RoleUpdater.Output {
    private final Config config = Config.getInstance();
    private final DatabaseManager databaseManager;
    private final MessageCooldownHandler messageCooldownHandler;
    private final RoleUpdater roleUpdater;
    private MessageEmbed  response = new EmbedBuilder().addField("Status", "No operations have been completed yet..", true).build();
    private EmbedBuilder detailedResponse = new EmbedBuilder(response);
    private long timeOfUpdate;
    private boolean command;

    /**
     * @param bot SequenceBuilder provides instances of the required classes to build the threads that can be used to create more instances
     * while still maintaining only one connection to the database at any one time.
     * @see DatabaseManager
     * @see DatabaseAccessListener
     * @see Bot
     */
    public ForceUpdate(Bot bot) {
        databaseManager = bot.getDatabaseManager();
        messageCooldownHandler = bot.getMessageCooldownHandler();
        roleUpdater = new RoleUpdater(true);
    }

    /**
     * This method does one iteration of the cycle that the main class initiates.
     * @param e The MessageReceivedEvent object received when a user sends a message.
     * @see Main
     * @see Bot
     */
    @SubscribeEvent
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        // I wish you would use a proper command handler and a configurable prefix.
        // command handler? nah im good /s. Good point might make one in the future.
        final char prefix = Config.getInstance().getPrefix();
        if (!e.getMessage().getContentRaw().equalsIgnoreCase(prefix + "update") && !e.getMessage()
                .getContentRaw()
                .equalsIgnoreCase(prefix + "updateinfo")) return;
        if (e.getAuthor().isBot()) return;
        if (e.getMessage().isWebhookMessage()) return;

        if (e.getChannel().getType() == ChannelType.PRIVATE) {
            if (e.getMessage().getContentRaw().equalsIgnoreCase(prefix + "update")) {
                return;
            }
            dmOutput(e);
            return;
        }
        if (e.getMessage().getContentRaw().equalsIgnoreCase(prefix + "updateinfo")) {
            e.getChannel().sendMessage("Please dm me this command!").queue();
            return;
        }
        if (e.getMember() == null) return; // should be after the above if because it will always be null if its a private channel.. good point

        List<Role> roles = e.getMember().getRoles();
        boolean carryOn = false;
        for (Role role : roles) { // cleaned up this 'satanic' code at the request of Xemor. That 'satanic' code didn't actually work as well, thank you Xemor.
            if (Arrays.asList(config.getRolesAllowedToUpdate()).contains(role.getIdLong())) {
                carryOn = true;
                break;
            }
        }
        if(!carryOn) {
            return;
        }
        final ForceUpdate instance = this;
        ScheduledExecutorService service = DatabaseManager.getService();

        roleUpdater.removeListener(instance);
        service.schedule(() -> {
            RoleUpdateQuery roleUpdateQuery = new RoleUpdateQuery(databaseManager);
            roleUpdater.addListener(instance);
            roleUpdateQuery.addListener(roleUpdater);
            MessageUpdater messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
            messageUpdater.run();
            e.getChannel().sendMessage(response).queue();
        }, 0, TimeUnit.MILLISECONDS);
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
