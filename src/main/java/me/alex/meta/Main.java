package me.alex.meta;

import me.alex.sql.DatabaseManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import java.util.Objects;
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
        System.setProperty("log4j2.configurationFile", Objects.requireNonNull(Main.class.getResource("/log4j.xml")).toString());
        Logger logger = ((Logger)LogManager.getRootLogger());
        logger.setLevel(Level.ALL);
        logger.getAppenders().keySet().forEach(System.out::println);
        org.apache.logging.log4j.Logger log = LogManager.getRootLogger();
        log.trace("pog");
        try {
            Config.loadConfig();
            Bot bot = new Bot();
            DatabaseManager databaseManager = bot.getDatabaseManager();
            databaseManager.firstTimeDatabaseSetup(bot);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        //service.scheduleAtFixedRate(() -> System.out.println("Test"), 0, 10000, TimeUnit.MILLISECONDS);
    }

    public static void startRunning(Bot bot) {
        DatabaseManager.getService().scheduleAtFixedRate(() -> bot.getMessageUpdater().run(),
                0,
                Config.getInstance().getDelay(),
                TimeUnit.MILLISECONDS);
    }
}