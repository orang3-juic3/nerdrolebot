package me.alex;

import me.alex.sql.DatabaseManager;
import net.dv8tion.jda.api.JDA;

import java.util.concurrent.TimeUnit;

/**
 * The main class of the project.
 */
// TODO: 20/12/2020 make all the listeners more generalised
public class Main {

    /**
     * The main method of the program.
     * @param args Takes in launch arguments.
     */
    public static void main(String[] args) {
        try {
            Config.loadConfig();
            Bot bot = new Bot();
            DatabaseManager databaseManager = bot.getDatabaseManager();
            databaseManager.firstTimeDatabaseSetup(bot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startRunning(Bot bot) {
        DatabaseManager.getService().scheduleAtFixedRate(() -> bot.getMessageUpdater().run(),
                0,
                Config.getInstance().delay,
                TimeUnit.MILLISECONDS);
    }
}