package me.alex.discord;

import me.alex.meta.Config;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Stream;

public class Blacklist {
    private final Config config = Config.getInstance();

    @SubscribeEvent
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        delete(e.getMessage(), e.getGuild());
    }

    @SubscribeEvent
    public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent e) {
        delete(e.getMessage(), e.getGuild());

    }
    private void delete(Message message, Guild guild) {
        final String content = message.getContentRaw();
        boolean shouldReturn = true;
        for (String i: config.getBlacklist()) {
            if (content.contains(i)) {
                shouldReturn = false;
                break;
            }
        }
        if (shouldReturn) return;
        if (!guild.getId().equals(String.valueOf(config.getServerId()))) return;
        final User author = message.getAuthor();
        if (Arrays.asList(config.getExemptionList()).contains(author.getIdLong())) return;
        message.delete()
                .queue(clazz -> author.openPrivateChannel()
                        .queue(privateChannel -> privateChannel.sendMessage("We deleted your message because it " +
                                "contained a prohibited keyword. " +
                                "Please contact the moderation team " +
                                "if you believe this was in error.")
                                .queue()),
                        error -> LogManager.getRootLogger().error(String.format("Error when moderating %s: [Message Content: %s]", author.getName(), message.getContentRaw())));
    }
    @SubscribeEvent
    public void onGuildUpdateName(GuildMemberUpdateNicknameEvent e) {
        if (e.getGuild().getIdLong() != config.getServerId()) return;
        boolean invalid = Stream.of(config.getBlacklist()).anyMatch(it -> e.getNewNickname() != null && e.getNewNickname().contains(it));
        if (invalid) {
            e.getMember().modifyNickname(e.getOldNickname() == null ? "" : e.getOldNickname()).queue();
            e.getJDA().openPrivateChannelById(e.getMember().getUser().getId()).queue(chn -> chn.sendMessage("We deleted your message because it " +
                    "contained a prohibited keyword. " +
                    "Please contact the moderation team " +
                    "if you believe this was in error.").queue(it -> LogManager.getRootLogger().info(String.format("Reverted %s's name!", e.getMember().getUser().getName())),
                    thr -> LogManager.getRootLogger().warn(String.format("Reverted %s's name, but couldn't dm them!", e.getMember().getUser().getName()))));
        }
    }
}
