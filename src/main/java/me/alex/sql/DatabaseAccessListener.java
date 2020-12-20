package me.alex.sql;

/**
 * An interface that allows classes to be notified of changes to the database's state of access.
 * @see DatabaseConnectionManager
 */
public interface DatabaseAccessListener {
    void onDatabaseAccessEvent();
    void onDatabaseStopAccessEvent();
}
