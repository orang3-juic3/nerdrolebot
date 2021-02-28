package me.alex.listeners;

import me.alex.sql.RoleUpdateQuery;

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
    default void onScoreMapReadyEvent(HashMap<Long, Long> scoreMap) {

    }
    default void onFullScoreMapReadyEvent(HashMap<Long, Long> fullScoreMap) {

    }
}
