package de.szalkowski.activitylauncher.todo;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import de.szalkowski.activitylauncher.R;
import de.szalkowski.activitylauncher.services.MyActivityInfo;

public class RootLauncherIconCreator {
    public static void createLauncherIcon(Context context, MyActivityInfo activity) {
        Signer signer = new Signer(context);
        Bundle extras = new Bundle();
        ComponentName comp = activity.getComponentName();

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

        // FIXME
//        activity.is_private = true;
//        activity.component_name = new ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.RootLauncherActivity");
//
//        IconCreator.createLauncherIcon(context, activity, extras);
    }
}
