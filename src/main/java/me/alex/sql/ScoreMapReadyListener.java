package me.alex.sql;

import java.util.HashMap;

public interface ScoreMapReadyListener {
    void onScoreMapReadyEvent(HashMap<Long, Long> scoreMap);
}
