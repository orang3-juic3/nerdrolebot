package me.alex.discord;

import me.alex.meta.Bot;
import me.alex.meta.Config;
import me.alex.meta.InvalidConfigurationException;
import me.alex.listeners.ScoreMapReadyListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import static java.util.Objects.requireNonNull;

public class RetrieveLeaderboard implements ScoreMapReadyListener {

    private final String thumbnail = "https://media.discordapp.net/attachments/787351993735708758/790033548525830144/nerdbot2.png?width=586&height=586";
    private final EmbedBuilder template = new EmbedBuilder().setColor(Color.GREEN).setTitle("Leaderboard").setAuthor("Nerd Bot", thumbnail);
    private HashMap<Long, Long> scoreMap = null;
    private final Config config = Config.getInstance();
    private final JDA jda = Bot.getJDA();
    private List<Member> members = new ArrayList<>();
    public RetrieveLeaderboard() {
    }

    @Override
    synchronized public void onFullScoreMapReadyEvent(HashMap<Long, Long> fullScoreMap) { // so we don't change the fullscore while it is being read.
        scoreMap = fullScoreMap;
        final Guild guild = jda.getGuildById(config.getServerId());
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
        if (firstIndex >= members.size() || firstIndex < 0) return createErrorEmbed();
        final StringBuilder builder = new StringBuilder();
        for (int i = firstIndex; i < lastIndex && i < members.size(); i++) {
            final Member member = members.get(i);
            final String name = MarkdownSanitizer.escape(member.getEffectiveName());
            builder.append(String.format("%s. %s with %s messages\n", i + 1, name, scoreMap.getOrDefault(member.getIdLong(), 0L)));
        }
        return new EmbedBuilder(template).addField("Page " + page, builder.toString(),false).build();
    }
    // TODO: 13/04/2021 Fix issue with discord parsing \\\\ as \\

    private MessageEmbed createEmbed(final List<Member> members) {
        final StringBuilder contents = new StringBuilder();
        for (Member member : members) {
            final String name = MarkdownSanitizer.escape(member.getEffectiveName());
            long numberOfMessages = scoreMap.getOrDefault(member.getIdLong(), 0L);
            if (numberOfMessages == 0) contents.append(String.format("%s has not sent any messages.", name));
            else contents.append(String.format("%s. %s with %s messages\n", this.members.indexOf(member) + 1, name, numberOfMessages));
        }
        return new EmbedBuilder(template).addField("Results", contents.toString(), false).build();
    }

    @SubscribeEvent
    public void onMessageReceived(@NotNull SlashCommandInteractionEvent e) {

        final char prefix = Config.getInstance().getPrefix();
        if (!e.getName().equals("leaderboard")) return;
        if (scoreMap == null) {
            e.reply("We have no data yet! Try running !update.").queue();
            return;
        }
        Guild guild = e.getJDA().getGuildById(config.getServerId());

        if (guild == null) {
            try {
                e.reply("We encountered an internal exception while handling your request.").queue();
                throw new InvalidConfigurationException("Server cannot be null!");
            } catch (InvalidConfigurationException ex) {
                ex.printStackTrace();
                e.getChannel().sendMessage("Cannot retrieve leaderboard because the guildId was specified incorrectly in the config!").queue();
                return;
            }
        }
        if ("user".equals(e.getSubcommandName())) {
            final OptionMapping userMapping = e.getOption("User");
            OptionMapping posMapping = e.getOption("Place");
            User target;
            if (userMapping == null && posMapping == null) {
                target = e.getUser();
            } else if (userMapping != null && posMapping != null) {
                e.replyEmbeds(new EmbedBuilder().setColor(Color.RED).setAuthor("Nerd Bot", thumbnail).setTimestamp(Instant.now()).setTitle("Uh-oh!").addField("Error:", "Invalid args", false).build()).queue();
                return;
            } else if (userMapping != null) {

            } else if (posMapping != null) {

            }
            if (guild.getMembers())
            e.replyEmbeds(createEmbed(List.of(target)))

            final List<Member> mentionedMembers = e.getMessage().getMentions().getMembers();
            final List<Member> membersToDisplay = new ArrayList<>(mentionedMembers);
            if (splitMessage.length == 1 && mentionedMembers.isEmpty()) {
                membersToDisplay.add(e.getMember());
            }
            if (!membersToDisplay.isEmpty()) { e.getChannel().sendMessageEmbeds(createEmbed(membersToDisplay)).queue(); return; }
            if (splitMessage[1].matches("[0-9]+")) {
                try {
                    int page = Integer.parseInt(splitMessage[1]);
                    e.getChannel().sendMessageEmbeds(createPage(page)).queue();
                } catch (NumberFormatException ex) {
                    e.getChannel().sendMessageEmbeds(createErrorEmbed()).queue();
                }
            }
        } else {
            e.replyEmbeds(createPage(requireNonNull(e.getOption("Page")).getAsInt())).queue();
        }


    }
}
