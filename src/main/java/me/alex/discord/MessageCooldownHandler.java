package me.alex.discord;

import me.alex.ConfigurationValues;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageCooldownHandler extends ListenerAdapter {
    private final ConfigurationValues configurationValues;
    private final JDA jda;
    public MessageCooldownHandler(ConfigurationValues configurationValues, JDA jda) {
        this.configurationValues = configurationValues;
        this.jda = jda;
    }
    private final HashMap<User,Long> cooldowns = new HashMap<>();
    private final ArrayList<User> users = new ArrayList<>();
    private final ArrayList<Long> timeStamp = new ArrayList<>();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!e.getGuild().equals(jda.getGuildById(configurationValues.serverId))) {
            System.out.println("Not the same guild");
            return;
        }
        long time = System.currentTimeMillis();

        if (cooldowns.get(e.getAuthor()) == null || cooldowns.get(e.getAuthor()) <= System.currentTimeMillis()) {
            users.add(e.getAuthor());
            timeStamp.add(time);
            cooldowns.put(e.getAuthor(), time + configurationValues.messageCooldown);
        }
    }

    public ArrayList<String> generateSqlCalls() {
        ArrayList<String> sqlCalls = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            sqlCalls.add(String.format("INSERT INTO messages VALUES (%s, %s)", users.get(i).getIdLong(), timeStamp.get(i)));
        }
        return sqlCalls;
    }
}
