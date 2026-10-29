package dev.playhorizon.horizon;

import android.app.NativeActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class HorizonGameActivity extends NativeActivity {
    private static final String TAG = "Horizon";

    public static native void onNativeGamepadKey(int keyCode, boolean pressed);
    public static native void onNativeGamepadAxis(float lx, float ly, float rx, float ry, float lt, float rt, float hatX, float hatY);

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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int source = event.getSource();
        int keyCode = event.getKeyCode();
        if ((source & InputDevice.SOURCE_GAMEPAD) != 0 
                || (source & InputDevice.SOURCE_JOYSTICK) != 0
                || (keyCode >= KeyEvent.KEYCODE_BUTTON_A && keyCode <= KeyEvent.KEYCODE_BUTTON_MODE)
                || (keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_CENTER)) {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            try {
                onNativeGamepadKey(keyCode, down);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to send native gamepad key", t);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_JOYSTICK) != 0 || (source & InputDevice.SOURCE_GAMEPAD) != 0) {
            float lx = event.getAxisValue(MotionEvent.AXIS_X);
            float ly = event.getAxisValue(MotionEvent.AXIS_Y);
            float rx = event.getAxisValue(MotionEvent.AXIS_Z);
            float ry = event.getAxisValue(MotionEvent.AXIS_RZ);
            float lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
            float rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
            if (lt == 0.0f) lt = event.getAxisValue(MotionEvent.AXIS_BRAKE);
            if (rt == 0.0f) rt = event.getAxisValue(MotionEvent.AXIS_GAS);
            float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

            try {
                onNativeGamepadAxis(lx, ly, rx, ry, lt, rt, hatX, hatY);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to send native gamepad axis", t);
            }
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }
}
