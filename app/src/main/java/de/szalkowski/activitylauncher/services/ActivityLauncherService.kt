package org.thirdparty;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.szalkowski.activitylauncher.R;

public class Launcher {
    /**
     * Got reference from stackoverflow.com URL
     * https://stackoverflow.com/questions/9194725/run-android-program-as-root
     * https://stackoverflow.com/questions/12343227/escaping-bash-function-arguments-for-use-by-su-c
     */
    public static void launchActivity(Context context, ComponentName activity, boolean asRoot, boolean showToast) {
        Intent intent = IconCreator.getActivityIntent(activity, null);

        if (showToast)
            Toast.makeText(context, String.format(context.getText(R.string.starting_activity).toString(), activity.flattenToShortString()),
                    Toast.LENGTH_LONG).show();
        try {
            if (!asRoot) {
                context.startActivity(intent);
            } else {
                startRootActivity(context, activity);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getText(R.string.error).toString() + ": " + e, Toast.LENGTH_LONG).show();
        }
    }

    private static void startRootActivity(Context context, ComponentName activity) throws IOException, InterruptedException, IllegalArgumentException {
        var component = activity.flattenToShortString();
        boolean isValid = validateComponentName(component);
        if (!isValid) {
            throw new IllegalArgumentException(String.format(context.getString(R.string.exception_invalid_component_name), component));
        }
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "am start -n " + component});
        String output = getProcessOutput(process);

        var exitValue = process.waitFor();
        if (exitValue > 0) {
            throw new RuntimeException(String.format(context.getString(R.string.exception_command_error), exitValue, output));
        }
    }
    /**
     * Got reference from stackoverflow.com URL:
     * https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
     */
    @NonNull
    private static String getProcessOutput(Process process) throws IOException {
        var stream = process.getErrorStream();
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }

    /**
     * In order to be on the safe side, validate component name before merging it into a root shell command
     *
     * @param component component name
     * @return true, if valid
     */
    private static boolean validateComponentName(String component) {
        Pattern p = Pattern.compile("^[./a-zA-Z0-9]+$");
        Matcher m = p.matcher(component);
        return m.matches();
    }
}
