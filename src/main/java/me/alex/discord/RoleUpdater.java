package me.alex.discord;

import me.alex.ConfigurationValues;
import me.alex.sql.ScoreMapReadyListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class actually updates people's roles
 */
public class RoleUpdater implements ScoreMapReadyListener {

    private final JDA jda;
    private final ConfigurationValues configurationValues = ConfigurationValues.getInstance();
    private HashMap<Long, Long> scoreMap;
    private final ArrayList<RoleUpdater.Output> listeners = new ArrayList<>();
    private final boolean command;

    /**
     * @param jda The JDA instance being used for this bot.
     * @param command Checks whether the update is called by a discord user.
     * @see JDA
     * @see ConfigurationValues
     * @see net.dv8tion.jda.api.entities.User
     */
    public RoleUpdater(JDA jda, boolean command) {
        this.command = command;
        this.jda = jda;
    }

    /**
     * @param output Adds a listener that is interested in knowing when output of the operation is available.
     */
    public void addListener(RoleUpdater.Output output) {
        listeners.add(output);
    }
    public void removeListener(RoleUpdater.Output output) {
        listeners.remove(output);
    }

    /**
     * @param scoreMap A hashmap with User IDs and the amount of messages they have sent.
     * This method gets called when RoleUpdateQuery has queried the database and compiled the results to a HashMap.
     * @see me.alex.sql.RoleUpdateQuery
     * @see HashMap
     */
    @Override
    public void onScoreMapReadyEvent(HashMap<Long, Long> scoreMap) {
        if (scoreMap == null || scoreMap.size() == 0) return;
        scoreMap.keySet().removeAll(Arrays.asList(configurationValues.exemptionList));
        this.scoreMap = scoreMap;
        Guild guild = jda.getGuildById(configurationValues.serverId);
        if (guild == null) {
            System.err.printf("Unknown server for id \"%s\"!%n", configurationValues.serverId);
            return;
        }
        guild.loadMembers().onSuccess(this::memberLoadCallback);
    }


    /**
     * @param members The full list of members as per Guild#loadMembers
     * This is a callback method that gets called when the list of members in the guild is ready. It then removes or adds the role specified
     * in ConfigurationValues depending on user's position within the top X percent of message senders, also defined in ConfigurationValues.
     * If the operation was called by a user, it will give feedback, and detailed feedback will always be available via DM.
     * @see net.dv8tion.jda.api.entities.User
     * @see ConfigurationValues
     * @see net.dv8tion.jda.api.utils.concurrent.Task
     * @see Role
     */
    public void memberLoadCallback(List<Member> members) {
        Guild guild = this.jda.getGuildById(this.configurationValues.serverId);
        if (guild == null) {
            System.err.printf("Unknown server for id \"%s\"!%n", this.configurationValues.serverId);
            return;
        }
        Role role = guild.getRoleById(this.configurationValues.roleId);
        if (role == null) {
            System.err.printf("Unknown role for id \"%s\"!%n", this.configurationValues.roleId);
            return;
        }
        List<Member> originalMembers = new ArrayList<>(members);
        members.removeIf(member -> member.getUser().isBot());
        members.removeIf(member -> scoreMap.getOrDefault(member.getIdLong(), 0L) == 0);
        members.sort(Comparator.comparingLong((member) -> scoreMap.get(member.getIdLong())));
        Collections.reverse(members);
        long messageMembersCount = members.size();
        long topMembers = (long) Math.ceil(messageMembersCount * (configurationValues.topPercentage /100));
        members = members.subList(0, (int) topMembers + 1);
        List<Member> rolesAdded = new ArrayList<>();
        List<Member> rolesRemoved = new ArrayList<>();
        for (Member member : members) {
            if (!member.getRoles().contains(role)) {
                guild.addRoleToMember(member, role).queue();
                rolesAdded.add(member);
            }
        }
        originalMembers.removeAll(members);
        for (Member member : originalMembers) {
            if (member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).queue();
                rolesRemoved.add(member);
            }
        }
        RolesChanged rolesChanged = new RolesChanged(rolesAdded, rolesRemoved);
        for (Output i : listeners) {
            i.onEmbedOutputReady(rolesChanged.getOutput());
            i.onAdvancedEmbedOutputReady(rolesChanged.getOutputDetailed(), rolesChanged.time, command);
        }
    }
    public static String getTimeFormatted(long millis) {
        return String.format("This operation finished %02d:%02d:%02ds ago",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    /**
     * An inner class that generates feedback
     */
    private class RolesChanged  {
        private final List<Member> rolesAdded;
        private final List<Member> rolesRemoved;
        private final Long time;

        /**
         * @param rolesAdded A list of Members that have had the role defined in ConfigurationValues added to them.
         * @param rolesRemoved A list of Members that have had the role defined in ConfigurationValues removed from them.
         * @see Member
         * @see ConfigurationValues
         */
        public RolesChanged(List<Member> rolesAdded, List<Member> rolesRemoved) {
            this.rolesAdded = rolesAdded;
            this.rolesRemoved = rolesRemoved;
            this.time = System.currentTimeMillis();
        }

        /**
         * @return Gets the basic output of the operation, ie how many people were given the role and how many had the role removed as an embed.
         * @see MessageEmbed
         * @see EmbedBuilder
         */
        public MessageEmbed getOutput() {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor("Nerd Bot", "https://media.discordapp.net/attachments/787351993735708758/790033548525830144/nerdbot2.png?width=586&height=586");
            embedBuilder.setThumbnail("https://media.discordapp.net/attachments/772164567425220640/789866299618099230/NerdBot.png?width=669&height=669");
            embedBuilder.setColor(new Color(52, 153, 49));
            embedBuilder.setTitle("Update Output:");
            Role role = jda.getRoleById(configurationValues.roleId);
            if (role == null) {
                return new EmbedBuilder().addField("Error:", "NullPointerException: Could not find role with ID " + configurationValues.roleId, true).build();
            }
            embedBuilder.addField("Changes:", "Users given role " + role.getName() + ": " + rolesAdded.size() +
                                 "\nUsers that had role " + role.getName() + " removed: " + rolesRemoved.size(), true);
            return embedBuilder.build();
        }

        /**
         * @return Gets the more detailed output, ie the basic output but with lists of the people who have had their roles changed, how long ago the operation was executed, and the source of the operation
         * @see RolesChanged#getOutput()
         */
        public EmbedBuilder getOutputDetailed() {
            Role role = jda.getRoleById(configurationValues.roleId);
            if (role == null) {
                return new EmbedBuilder().addField("Error:", "NullPointerException: Could not find role with ID " + configurationValues.roleId, true);
            }

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor("Nerd Bot", "https://discord.com/channels/787351993285869589/787351993735708758/790033548732006410");
            embedBuilder.setThumbnail("https://media.discordapp.net/attachments/772164567425220640/789866299618099230/NerdBot.png?width=669&height=669");
            embedBuilder.setColor(new Color(52, 153, 49));
            embedBuilder.setTitle("Update Output Detailed:");
            embedBuilder.addField("Changes:", "Users given role " + role.getName() + ": " + rolesAdded.size() +
                    "\nUsers that had role " + role.getName() + " removed: " + rolesRemoved.size(), true);
            String listGiven = rolesAdded.stream().map(Member::getEffectiveName).map(Objects::toString).collect(Collectors.joining(", ")); // concatenate all members to single string
            embedBuilder.addField("List of users given the role:", listGiven, true);
            String listRemoved = rolesRemoved.stream().map(Member::getEffectiveName).map(Objects::toString).collect(Collectors.joining(", "));
            embedBuilder.addField("List of users where the role was removed:", listRemoved, true);
            return embedBuilder;
        }
    }

    /**
     * An interface that notifies listeners who are interested in receiving the output from the RoleUpdate operation.
     * @see ForceUpdate
     * @see RoleUpdater
     */
    public interface Output {
        default void onAdvancedEmbedOutputReady(EmbedBuilder embedBuilder, long time, boolean command) {

        }
        default void onEmbedOutputReady(MessageEmbed messageEmbed) {
        }
    }
}
