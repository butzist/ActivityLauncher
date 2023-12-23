package de.szalkowski.activitylauncher.todo;

import java.io.File;
import java.util.Objects;
import java.util.Vector;

public class RootDetection {
    public static boolean detectSU() {
        Vector<File> paths = new Vector<>();
        String[] dirs = Objects.requireNonNull(System.getenv("PATH")).split(":");
        for (String dir : dirs) {
            paths.add(new File(dir, "su"));
        }

        for (File path : paths) {
            if (path.exists() && path.canExecute() && path.isFile()) {
                return true;
            }
        }

        return false;
    }


}
