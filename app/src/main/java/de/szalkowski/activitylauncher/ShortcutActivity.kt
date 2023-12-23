package de.szalkowski.activitylauncher;

import static org.thirdparty.Launcher.launchActivity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URISyntaxException;

public class ShortcutActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent launchIntent = Intent.parseUri(getIntent().getStringExtra("extra_intent"), 0);
            launchActivity(this, launchIntent.getComponent(), false, false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            finish();
        }
    }
}
