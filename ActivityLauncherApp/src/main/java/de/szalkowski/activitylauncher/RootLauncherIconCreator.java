package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import org.thirdparty.IconCreator;

public class RootLauncherIconCreator {
    public static void createLauncherIcon(Context context, MyActivityInfo activity) {
        var signer = new Signer(context);
        var extras = new Bundle();
        var comp = activity.getComponentName();

        String signature;
        try {
            signature = signer.signComponentName(comp);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getText(R.string.error).toString() + ": " + e, Toast.LENGTH_LONG).show();
            return;
        }

        extras.putString("pkg", comp.getPackageName());
        extras.putString("cls", comp.getClassName());
        extras.putString("sign", signature);

        activity.is_private = true;
        activity.component_name = new ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.RootLauncherActivity");

        IconCreator.createLauncherIcon(context, activity, extras);
    }
}
