package me.alex;

import java.util.ArrayList;
import java.util.Scanner;

public class InputThread implements Runnable {
    ArrayList<Close> listeners = new ArrayList<>();
    public void addListener(Close listener) {
        listeners.add(listener);
    }
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type in any letter to exit");
        scanner.next();
        for (Close i: listeners) {
            i.stopProgram();
        }
    }
    public interface Close {
        void stopProgram();
    }
}
