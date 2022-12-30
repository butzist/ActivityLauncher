package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.thirdparty.Launcher;

public class RootLauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Bundle bundle = getIntent().getExtras();
            if (bundle == null) {
                return;
            }

            String pkg = bundle.getString("pkg");
            String cls = bundle.getString("cls");
            String signature = bundle.getString("sign");

            var componentName = new ComponentName(pkg, cls);

            var signer = new Signer(getApplicationContext());
            if (signer.validateComponentNameSignature(componentName, signature)) {
                Launcher.launchActivity(getApplicationContext(), componentName, true, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getText(R.string.error).toString() + ": " + e, Toast.LENGTH_LONG).show();
        } finally {
            finish();
        }
    }
}
