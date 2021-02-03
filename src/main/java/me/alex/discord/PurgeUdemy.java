package me.alex.discord;

import me.alex.ConfigurationValues;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PurgeUdemy extends ListenerAdapter {
    private final ConfigurationValues config = ConfigurationValues.getInstance();
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        if (!e.getMessage().getContentRaw().contains("udemy.com/course")) return;
        if (!e.getGuild().getId().equals(String.valueOf(config.serverId))) return;
        if (Arrays.asList(config.exemptionList).contains(e.getAuthor().getIdLong())) return;
        e.getMessage().delete()
                .queue(clazz -> e.getAuthor().openPrivateChannel()
                .queue(privateChannel -> privateChannel.sendMessage("We deleted your message because it " +
                        "contained a udemy link. " +
                        "Please contact the moderation team " +
                        "if you believe this was in error.")
                .queue()));

    }
}
