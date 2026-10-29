package dev.playhorizon.horizon;

import android.app.NativeActivity;
import android.os.Bundle;
import android.util.Log;

public class HorizonGameActivity extends NativeActivity {
    private static final String TAG = "Horizon";

    static {
        try {
            Log.i(TAG, "HorizonGameActivity: Loading native library horizon_client...");
            System.loadLibrary("horizon_client");
            Log.i(TAG, "HorizonGameActivity: Successfully loaded horizon_client!");
        } catch (Throwable t) {
            Log.e(TAG, "HorizonGameActivity: Failed to load horizon_client", t);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "HorizonGameActivity onCreate");
        super.onCreate(savedInstanceState);
    }
}
