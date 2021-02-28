package me.alex.listeners;

import me.alex.sql.DatabaseManager;

/**
 * An interface that allows classes to be notified of changes to the database's state of access.
 * @see DatabaseManager
 */
public interface DatabaseAccessListener {
    void onDatabaseAccessEvent();
    void onDatabaseStopAccessEvent();
}
