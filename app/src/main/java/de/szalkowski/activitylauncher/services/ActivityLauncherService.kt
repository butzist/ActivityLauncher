package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ActivityContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.internal.getActivityIntent
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.inject.Inject


interface ActivityLauncherService {
    fun launchActivity(
        activity: ComponentName,
        asRoot: Boolean,
        showToast: Boolean
    )
}

class ActivityLauncherServiceImpl @Inject constructor(@ActivityContext private val context: Context) :
    ActivityLauncherService {
    /**
     * Got reference from stackoverflow.com URL
     * https://stackoverflow.com/questions/9194725/run-android-program-as-root
     * https://stackoverflow.com/questions/12343227/escaping-bash-function-arguments-for-use-by-su-c
     */
    override fun launchActivity(
        activity: ComponentName,
        asRoot: Boolean,
        showToast: Boolean
    ) {
        val intent = getActivityIntent(activity, null)
        if (showToast) Toast.makeText(
            context,
            String.format(
                context.getText(R.string.starting_activity).toString(),
                activity.flattenToShortString()
            ),
            Toast.LENGTH_LONG
        ).show()
        try {
            if (!asRoot) {
                context.startActivity(intent)
            } else {
                startRootActivity(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getText(R.string.error).toString() + ": " + e,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Throws(IOException::class, InterruptedException::class, IllegalArgumentException::class)
    private fun startRootActivity(activity: ComponentName) {
        val component = activity.flattenToShortString()
        val isValid = validateComponentName(component)
        require(isValid) {
            String.format(
                context.getString(R.string.exception_invalid_component_name),
                component
            )
        }

        val process = Runtime.getRuntime().exec(
            arrayOf(
                "su", "-c",
                "am start -n $component"
            )
        )
        val output = getProcessOutput(process)
        val exitValue = process.waitFor()
        if (exitValue > 0) {
            throw RuntimeException(
                String.format(
                    context.getString(R.string.exception_command_error),
                    exitValue,
                    output
                )
            )
        }
    }

    /**
     * Got reference from stackoverflow.com URL:
     * https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
     */
    @Throws(IOException::class)
    private fun getProcessOutput(process: Process): String {
        val stream = process.errorStream
        val bufferSize = 1024
        val buffer = CharArray(bufferSize)
        val out = StringBuilder()
        val `in`: Reader = InputStreamReader(stream, StandardCharsets.UTF_8)
        var numRead: Int
        while (`in`.read(buffer, 0, buffer.size).also { numRead = it } > 0) {
            out.appendRange(buffer, 0, numRead)
        }
        return out.toString()
    }

    /**
     * In order to be on the safe side, validate component name before merging it into a root shell command
     *
     * @param component component name
     * @return true, if valid
     */
    private fun validateComponentName(component: String): Boolean {
        val p = Pattern.compile("^[./a-zA-Z0-9]+$")
        val m = p.matcher(component)
        return m.matches()
    }
}

