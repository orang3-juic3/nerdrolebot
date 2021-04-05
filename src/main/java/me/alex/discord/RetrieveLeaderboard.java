package me.alex.discord;

import me.alex.Config;
import me.alex.InvalidConfigurationException;
import me.alex.listeners.ScoreMapReadyListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;

public class RetrieveLeaderboard extends ListenerAdapter implements ScoreMapReadyListener {

    private final String thumbnail = "https://media.discordapp.net/attachments/787351993735708758/790033548525830144/nerdbot2.png?width=586&height=586";
    private final EmbedBuilder template = new EmbedBuilder().setColor(Color.GREEN).setTitle("Leaderboard").setAuthor("Nerd Bot", thumbnail);
    private HashMap<Long, Long> scoreMap = null;
    private final Config config = Config.getInstance();
    private final JDA jda;
    private List<Member> members = new ArrayList<>();
    public RetrieveLeaderboard(JDA jda) {
        this.jda = jda;
    }

    @Override
    synchronized public void onFullScoreMapReadyEvent(HashMap<Long, Long> fullScoreMap) { // so we don't change the fullscore while it is being read.
        scoreMap = fullScoreMap;
        final Guild guild = jda.getGuildById(config.serverId);
        if (guild == null) {
            try {
                throw new InvalidConfigurationException("Server cannot be null!");
            } catch (InvalidConfigurationException ex) {
                ex.printStackTrace();
                return;
            }
        }
        guild.loadMembers().onSuccess(members -> {
            members.removeIf(Objects::isNull);
            members.removeIf(member -> member.getUser().isBot());
            members.removeIf(member -> scoreMap.getOrDefault(member.getIdLong(), 0L) == 0);
            members.sort(Comparator.comparingLong((member) -> scoreMap.get(member.getIdLong())));
            Collections.reverse(members);
            this.members = members;
        });
    }

    private MessageEmbed createErrorEmbed() {
        return new EmbedBuilder().setColor(Color.RED).setAuthor("Nerd Bot", thumbnail).setTimestamp(Instant.now()).setTitle("Uh-oh!").addField("Error:", "Invalid page number!", false).build();
    }

    private MessageEmbed createPage(int page) {
        int firstIndex = (page - 1) * 10;
        int lastIndex = firstIndex + 10;
        if (firstIndex >= members.size()) return createErrorEmbed();
        StringBuilder builder = new StringBuilder();
        for (int i = firstIndex; i < lastIndex && i < members.size(); i++) {
            final Member member = members.get(i);
            final String name = MarkdownSanitizer.escape(member.getEffectiveName());
            builder.append(String.format("%s. %s with %s messages\n", i, name, scoreMap.getOrDefault(member.getIdLong(), 0L)));
        }
        return new EmbedBuilder(template).addField("Page " + page, builder.toString(),false).build();
    }

    private MessageEmbed createEmbed(final List<Member> members) {
        final StringBuilder contents = new StringBuilder();
        for (Member member : members) {
            final String name = MarkdownSanitizer.escape(member.getEffectiveName());
            long numberOfMessages = scoreMap.getOrDefault(member.getIdLong(), 0L);
            if (numberOfMessages == 0) contents.append(String.format("%s has not sent any messages.", name));
            else contents.append(String.format("%s. %s with %s messages\n", this.members.indexOf(member), name, numberOfMessages));
        }
        return new EmbedBuilder(template).addField("Results", contents.toString(), false).build();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        final char prefix = Config.getInstance().prefix;
        if (!e.getMessage().getContentRaw().startsWith(prefix + "lead") && !e.getMessage().getContentRaw().startsWith(prefix + "leaderboard")) return; //ensures it is the right command
        String[] splitMessage = e.getMessage().getContentRaw().split(" "); //splits into arguments
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
                e.getChannel().sendMessage("Cannot retrieve leaderboard because the guildId was specified incorrectly in the config!").queue();
                return;
            }
        }
        final List<Member> mentionedMembers = e.getMessage().getMentionedMembers();
        final List<Member> membersToDisplay = new ArrayList<>(mentionedMembers);
        if (splitMessage.length == 1 && mentionedMembers.isEmpty()) {
            membersToDisplay.add(e.getMember());
        }
        if (!membersToDisplay.isEmpty()) e.getChannel().sendMessage(createEmbed(membersToDisplay)).queue();
        if (splitMessage[1].matches("[0-9]+")) {
            int page = Integer.parseInt(splitMessage[1]);
            e.getChannel().sendMessage(createPage(page)).queue();
        }
    }
}
