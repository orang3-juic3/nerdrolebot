package me.alex.sql;

import java.util.HashMap;

/**
 * An interface to notify listeners that a hashmap with User IDs and the amount of messages they have sent is available.
 * @see net.dv8tion.jda.api.entities.User
 * @see me.alex.discord.RoleUpdater
 * @see RoleUpdateQuery
 */
public interface ScoreMapReadyListener {
    /**
     * @param scoreMap A hashmap with User IDs and the amount of messages they have sent.
     */
    void onScoreMapReadyEvent(HashMap<Long, Long> scoreMap);
}
