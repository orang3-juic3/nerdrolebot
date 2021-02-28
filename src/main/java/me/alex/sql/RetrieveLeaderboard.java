package me.alex.sql;

import me.alex.Config;
import me.alex.InvalidConfigurationException;
import me.alex.listeners.ScoreMapReadyListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class RetrieveLeaderboard extends ListenerAdapter implements ScoreMapReadyListener {
    private HashMap<Long, Long> scoreMap = null;
    private final Config config = Config.getInstance();

    @Override
    synchronized public void onFullScoreMapReadyEvent(HashMap<Long, Long> fullScoreMap) { // so we don change the fullscore while it is being read.
        scoreMap = fullScoreMap;
    }
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!e.getMessage().getContentRaw().startsWith("!lead") && !e.getMessage().getContentRaw().startsWith("!leaderboard")) return;
        String[] splitMessage = e.getMessage().getContentRaw().split(" ");
        if (scoreMap == null) {
            e.getChannel().sendMessage("We have no data yet! Try running !update.").queue();
            return;
        }
        Guild guild = e.getJDA().getGuildById(config.serverId);
        if (guild == null) {
            try {
                throw new InvalidConfigurationException("Server cannot be null!");
            } catch (InvalidConfigurationException ex) {
                ex.printStackTrace();
                e.getChannel().sendMessage("Cannot retrieve leaderboard because the guildId was specified incorrectly!").queue();
                return;
            }
        }
        guild.loadMembers().onSuccess(members -> {
            members.removeIf(Objects::isNull);
            List<Member> mentionedMembers = e.getMessage().getMentionedMembers();
            StringBuilder leaderboardMsg = new StringBuilder();
            String noMessages = "User %s hasn't sent any registered messages!";
            members.removeIf(member -> member.getUser().isBot());
            members.removeIf(member -> scoreMap.getOrDefault(member.getIdLong(), 0L) == 0);
            members.sort(Comparator.comparingLong((member) -> scoreMap.get(member.getIdLong())));
            Collections.reverse(members);
            final List<String> memberNames = members.stream()
                    .map(member -> MarkdownSanitizer.escape(member.getEffectiveName()))
                    .collect(Collectors.toList());
            if (splitMessage.length == 1 && mentionedMembers.isEmpty()) {
                Member author = e.getMember();
                if (author == null) {
                    e.getChannel().sendMessage("For some reason, you are null.").queue();
                    return;
                }
                final Long messages = scoreMap.get(author.getUser().getIdLong());
                if (messages == null) {
                    leaderboardMsg.append(String.format(noMessages, MarkdownSanitizer.escape(author.getEffectiveName())));
                } else {
                    leaderboardMsg.append(String.format("You are number %s on the leaderboard with %s messages.", members.indexOf(author) + 1, messages));
                }
                e.getChannel().sendMessage(leaderboardMsg.toString()).queue();
            } else if (!mentionedMembers.isEmpty()) {
                for (Member member : mentionedMembers) {
                    if (member == null) {
                        leaderboardMsg.append("A mentioned member is null.\n");
                        continue;
                    }
                    final String memberName = MarkdownSanitizer.escape(member.getEffectiveName());
                    int position = members.indexOf(member) + 1;
                    Long messages = scoreMap.get(member.getUser().getIdLong());
                    if (messages == null) {
                        leaderboardMsg.append(String.format(noMessages, memberName));
                        continue;
                    }
                    leaderboardMsg.append(String.format("%s is number %s on the leaderboard with %s messages\n",memberName, position, messages));
                }
                e.getChannel().sendMessage(leaderboardMsg.toString()).queue();
            } else if (splitMessage[1].matches("[0-9]+")) {
                int top = Integer.parseInt(splitMessage[1]);
                if (top > members.size()) {
                    e.getChannel().sendMessage("Sorry, this number is too large. Please try again.").queue();
                    return;
                } else if (top <= 0) {
                    e.getChannel().sendMessage("Sorry, this number is too small. Please try again").queue();
                    return;
                }
                for (int i = 0; i < top; i++) {
                    Member currentMember = members.get(i);
                    leaderboardMsg.append(String.format("%s. User %s with %s messages.\n", i + 1, memberNames.get(i), scoreMap.get(currentMember.getUser().getIdLong())));
                }
                if (leaderboardMsg.length() > 2000) {
                    e.getChannel().sendMessage("Sorry, this number is too large. Please try again.").queue();
                } else {
                    e.getChannel().sendMessage(leaderboardMsg.toString()).queue();
                }

            }
        });
    }
}
