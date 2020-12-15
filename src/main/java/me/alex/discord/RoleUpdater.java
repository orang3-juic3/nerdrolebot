package me.alex.discord;

import me.alex.sql.ScoreMapReadyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;

public class RoleUpdater implements ScoreMapReadyListener {

    private final JDA jda;

    public RoleUpdater(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onScoreMapReadyEvent(HashMap<Long, Long> scoreMap) {
        long avg = scoreMap.values().stream().mapToLong(Long::longValue).sum() / scoreMap.values().size();
        for (Long i: scoreMap.keySet()) {
            if (scoreMap.get(i) >= avg) {
                jda.retrieveUserById(i).map(User::getName).queue(name -> {
                    System.out.println("User " + name + " should have nerd role.");
                });
            } else {
                jda.retrieveUserById(i).map(User::getName).queue(name -> {
                    System.out.println("User " + name + " shouldn't have nerd role!");
                });
            }
        }
    }
}
