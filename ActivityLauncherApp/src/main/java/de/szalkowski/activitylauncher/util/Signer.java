package de.szalkowski.activitylauncher.util;

import android.content.ComponentName;
import android.content.Context;
import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Signer {
    private final String key;

    public Signer(Context context) {
        var preferences = context.getSharedPreferences("signer", Context.MODE_PRIVATE);
        if (!preferences.contains("key")) {
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[256];
            random.nextBytes(bytes);

            this.key = Base64.encodeToString(bytes, Base64.NO_WRAP);
            preferences.edit().putString("key", this.key).apply();
        } else {
            this.key = preferences.getString("key", "");
        }
    }

    /**
     * Adapted from StackOverflow:
     * https://stackoverflow.com/questions/36004761/is-there-any-function-for-creating-hmac256-string-in-android
     */
    private static String hmac256(String key, String message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
        byte[] result = mac.doFinal(message.getBytes());
        return Base64.encodeToString(result, Base64.NO_WRAP);
    }

    public String signComponentName(ComponentName comp) throws InvalidKeyException, NoSuchAlgorithmException {
        var name = comp.flattenToShortString();
        return hmac256(this.key, name);
    }

    public boolean validateComponentNameSignature(ComponentName comp, String signature) throws InvalidKeyException, NoSuchAlgorithmException {
        var compSignature = this.signComponentName(comp);
        return signature.equals(compSignature);
    }
}
