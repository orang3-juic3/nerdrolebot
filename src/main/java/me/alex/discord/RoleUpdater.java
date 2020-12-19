package me.alex.discord;

import me.alex.ConfigurationValues;
import me.alex.sql.ScoreMapReadyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RoleUpdater implements ScoreMapReadyListener {

    private final JDA jda;
    private final ConfigurationValues configurationValues;
    private HashMap<Long, Long> scoreMap;

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
        members = members.stream().filter(member -> {
            try {
                AtomicBoolean isBot = new AtomicBoolean(true);
                jda.retrieveUserById(member.getId()).queue(i -> {
                    if (i.isBot()) isBot.set(false);
                });
                return isBot.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }).collect(Collectors.toCollection(ArrayList::new));
        Guild guild = jda.getGuildById(configurationValues.serverId);
        if (guild == null) {
            System.err.println(String.format("Unknown server for id \"%s\"!", configurationValues.serverId));
            return;
        }
        Role role = guild.getRoleById(configurationValues.roleId);
        if (role == null) {
            System.err.println(String.format("Unknown role for id \"%s\"!", configurationValues.roleId));
            return;
        }
        for (Member i : members) {
            scoreMap.putIfAbsent(i.getIdLong(), 0L);
        }
        long sum = scoreMap.values().stream().mapToLong(Long::longValue).sum();
        long size = scoreMap.values().size();
        if (sum == 0 || size == 1) {
            return;
        }
        double avg = ((double) sum) / size;
        ArrayList<Long> memberIds = members.stream().map(ISnowflake::getIdLong).collect(Collectors.toCollection(ArrayList::new));
        for (Long i : scoreMap.keySet()) {
            Member member = guild.getMember(User.fromId(i));
            if (member == null) {
                continue;
            }
            List<Role> roles = member.getRoles();
            if (roles.contains(role)) {
                if (scoreMap.get(i) < avg) {
                    guild.removeRoleFromMember(member, role).queue();
                }
            } else {
                if (scoreMap.get(i) > avg) {
                    guild.addRoleToMember(member, role).queue();
                }
            }
        }
    }
}
