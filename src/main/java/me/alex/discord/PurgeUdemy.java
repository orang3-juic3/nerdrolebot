package me.alex.discord;

import me.alex.Config;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PurgeUdemy extends ListenerAdapter {
    private final Config config = Config.getInstance();
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        delete(e.getMessage(), e.getGuild());
    }
    @Override
    public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent e) {
        delete(e.getMessage(), e.getGuild());

    }
    private void delete(Message message, Guild guild) {
        if (!message.getContentRaw().contains("udemy.com/course")) return;
        if (!guild.getId().equals(String.valueOf(config.serverId))) return;
        final User author = message.getAuthor();
        if (Arrays.asList(config.exemptionList).contains(author.getIdLong())) return;
        message.delete()
                .queue(clazz -> author.openPrivateChannel()
                        .queue(privateChannel -> privateChannel.sendMessage("We deleted your message because it " +
                                "contained a udemy link. " +
                                "Please contact the moderation team " +
                                "if you believe this was in error.")
                                .queue()),
                        error -> System.err.printf("Error when moderating %s: [Message Content: %s]", author.getName(), message.getContentRaw()));
    }
}
