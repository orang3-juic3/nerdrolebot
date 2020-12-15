package me.alex.sql;

import java.util.ArrayList;

public class DatabaseConnectionManager {
    private final ArrayList<DatabaseAccessListener> databaseAccessListeners = new ArrayList<>();
    public void addListener(DatabaseAccessListener databaseAccessListener) {
        databaseAccessListeners.add(databaseAccessListener);
    }
    public void notifyAccess() {
        for (DatabaseAccessListener i : databaseAccessListeners) {
            i.onDatabaseAccessEvent();
        }
    }
    public void notifyStopAccess() {
        for (DatabaseAccessListener i : databaseAccessListeners) {
            i.onDatabaseStopAccessEvent();
        }
    }
}
