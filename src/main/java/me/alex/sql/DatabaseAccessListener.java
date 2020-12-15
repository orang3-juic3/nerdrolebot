package me.alex.sql;

public interface DatabaseAccessListener {
    void onDatabaseAccessEvent();
    void onDatabaseStopAccessEvent();

}
