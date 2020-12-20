package me.alex.sql;

import java.util.ArrayList;

/**
 * This is a class that helps deal with maintaining no concurrent connections to the database.
 * @see DatabaseAccessListener
 */
public class DatabaseConnectionManager {

    private final ArrayList<DatabaseAccessListener> databaseAccessListeners = new ArrayList<>();

    /**
     * @param databaseAccessListener Adds a listener that listens for when the database has been accessed, and when it has stopped being accessed.
     * @see DatabaseAccessListener
     */
    public void addListener(DatabaseAccessListener databaseAccessListener) {
        databaseAccessListeners.add(databaseAccessListener);
    }

    /**
     * Notifies listeners that the database is being accessed.
     * @see DatabaseAccessListener
     */
    synchronized public void notifyAccess() {
        for (DatabaseAccessListener i : databaseAccessListeners) {
            i.onDatabaseAccessEvent();
        }
    }

    /**
     * Notifies listeners that the database has stopped being accessed
     * @see DatabaseAccessListener
     */
    synchronized public void notifyStopAccess() {
        for (DatabaseAccessListener i : databaseAccessListeners) {
            i.onDatabaseStopAccessEvent();
        }
    }
}
