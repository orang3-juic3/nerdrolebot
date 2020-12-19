package me.alex.discord;

import me.alex.ConfigurationValues;
import me.alex.sql.ScoreMapReadyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class RoleUpdater implements ScoreMapReadyListener {

    private final JDA jda;
    private final ConfigurationValues configurationValues;
    private HashMap<Long, Long> scoreMap;
    double topPercentage = 0.5D;

    public RoleUpdater(JDA jda, ConfigurationValues configurationValues) {
        this.jda = jda;
        this.configurationValues = configurationValues;
    }

    @Override
    public void onScoreMapReadyEvent(HashMap<Long, Long> scoreMap) {
        if (scoreMap == null || scoreMap.size() == 0) return;
        this.scoreMap = scoreMap;
        Guild guild = jda.getGuildById(configurationValues.serverId);
        if (guild == null) {
            System.err.println(String.format("Unknown server for id \"%s\"!", configurationValues.serverId));
            return;
        }
        guild.loadMembers().onSuccess(this::memberLoadCallback);
    }


    public void memberLoadCallback(List<Member> members) {
        Guild guild = this.jda.getGuildById(this.configurationValues.serverId);
        if (guild == null) {
            System.err.println(String.format("Unknown server for id \"%s\"!", this.configurationValues.serverId));
            return;
        }
        Role role = guild.getRoleById(this.configurationValues.roleId);
        if (role == null) {
            System.err.println(String.format("Unknown role for id \"%s\"!", this.configurationValues.roleId));
            return;
        }
        members.removeIf((member) -> member.getUser().isBot());
        members.sort(Comparator.comparingLong((member) -> scoreMap.getOrDefault(member.getIdLong(), 0L)));
        long messageMembersCount = scoreMap.size();
        Collections.reverse(members);
        long topMembers = (long) Math.ceil(messageMembersCount * topPercentage);
        for(int i = 0; i < members.size(); ++i) {
            Member member = members.get(i);
            if (i < topMembers) {
                if (!member.getRoles().contains(role)) {
                    guild.addRoleToMember(member, role).queue();
                }
            } else if (member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).queue();
            }
        }
    }
}
