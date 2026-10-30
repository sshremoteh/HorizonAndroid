package dev.playhorizon.horizon;

import android.app.NativeActivity;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.system.Os;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class HorizonGameActivity extends NativeActivity implements InputManager.InputDeviceListener {
    private static final String TAG = "HorizonGamepad";

    public static native void onNativeGamepadKey(int keyCode, boolean pressed);
    public static native void onNativeGamepadAxis(float lx, float ly, float rx, float ry, float lt, float rt, float hatX, float hatY);

    static {
        try {
            Log.i("Horizon", "HorizonGameActivity: Loading native library horizon_client...");
            System.loadLibrary("horizon_client");
            Log.i("Horizon", "HorizonGameActivity: Successfully loaded horizon_client!");
        } catch (Throwable t) {
            Log.e("Horizon", "HorizonGameActivity: Failed to load horizon_client", t);
        }
    }

    private InputManager mInputManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Read intent extras passed from MainActivity launcher
        if (getIntent() != null) {
            String renderDistance = getIntent().getStringExtra("HORIZON_RENDER_DISTANCE");
            if (renderDistance != null && !renderDistance.isEmpty()) {
                try { Os.setenv("HORIZON_RENDER_DISTANCE", renderDistance, true); } catch (Throwable ignored) {}
            }
            String playerName = getIntent().getStringExtra("HORIZON_PLAYER_NAME");
            if (playerName != null && !playerName.isEmpty()) {
                try { Os.setenv("HORIZON_PLAYER_NAME", playerName, true); } catch (Throwable ignored) {}
            }
            String inFrame = getIntent().getStringExtra("HORIZON_SERVER_THREAD");
            if (inFrame != null && !inFrame.isEmpty()) {
                try { Os.setenv("HORIZON_SERVER_THREAD", inFrame, true); } catch (Throwable ignored) {}
            }
            String touchOpacity = getIntent().getStringExtra("HORIZON_TOUCH_OPACITY");
            if (touchOpacity != null && !touchOpacity.isEmpty()) {
                try { Os.setenv("HORIZON_TOUCH_OPACITY", touchOpacity, true); } catch (Throwable ignored) {}
            }
            String resourcesPath = getIntent().getStringExtra("HORIZON_RESOURCES");
            if (resourcesPath != null && !resourcesPath.isEmpty()) {
                try { Os.setenv("HORIZON_RESOURCES", resourcesPath, true); } catch (Throwable ignored) {}
            }
        }

        super.onCreate(savedInstanceState);
        Log.i(TAG, "HorizonGameActivity onCreate: initialized");

        try {
            mInputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            if (mInputManager != null) {
                mInputManager.registerInputDeviceListener(this, null);
                scanInputDevices();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to register input device listener", t);
        }

        // Attach listeners to DecorView to capture motions/keys before native queue if needed
        try {
            View decor = getWindow().getDecorView();
            decor.setFocusable(true);
            decor.setFocusableInTouchMode(true);
            decor.requestFocus();
            decor.setOnGenericMotionListener(new View.OnGenericMotionListener() {
                @Override
                public boolean onGenericMotion(View v, MotionEvent event) {
                    return handleGenericMotion(event);
                }
            });
            decor.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    return handleKeyEvent(event);
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to attach decor view listeners", t);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInputManager != null) {
            try {
                mInputManager.unregisterInputDeviceListener(this);
            } catch (Throwable ignored) {}
        }
    }

    private void scanInputDevices() {
        if (mInputManager == null) return;
        int[] ids = mInputManager.getInputDeviceIds();
        for (int id : ids) {
            InputDevice dev = mInputManager.getInputDevice(id);
            if (dev != null && !dev.isVirtual()) {
                int sources = dev.getSources();
                if ((sources & InputDevice.SOURCE_GAMEPAD) != 0 || (sources & InputDevice.SOURCE_JOYSTICK) != 0) {
                    Log.i(TAG, "Found Gamepad/Controller: " + dev.getName() + " (vendor=" + dev.getVendorId() + ", product=" + dev.getProductId() + ")");
                }
            }
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        Log.i(TAG, "Input device added: id=" + deviceId);
        scanInputDevices();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        Log.i(TAG, "Input device removed: id=" + deviceId);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {}

    private boolean isGamepadKey(int keyCode, int source) {
        return (source & InputDevice.SOURCE_GAMEPAD) != 0
                || (source & InputDevice.SOURCE_JOYSTICK) != 0
                || (keyCode >= KeyEvent.KEYCODE_BUTTON_A && keyCode <= KeyEvent.KEYCODE_BUTTON_MODE)
                || (keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_CENTER)
                || keyCode == KeyEvent.KEYCODE_BUTTON_1
                || keyCode == KeyEvent.KEYCODE_BUTTON_2
                || keyCode == KeyEvent.KEYCODE_BUTTON_3
                || keyCode == KeyEvent.KEYCODE_BUTTON_4
                || keyCode == KeyEvent.KEYCODE_BUTTON_5
                || keyCode == KeyEvent.KEYCODE_BUTTON_6
                || keyCode == KeyEvent.KEYCODE_BUTTON_7
                || keyCode == KeyEvent.KEYCODE_BUTTON_8
                || keyCode == KeyEvent.KEYCODE_BUTTON_9
                || keyCode == KeyEvent.KEYCODE_BUTTON_10
                || keyCode == KeyEvent.KEYCODE_BUTTON_11
                || keyCode == KeyEvent.KEYCODE_BUTTON_12
                || keyCode == KeyEvent.KEYCODE_BUTTON_13
                || keyCode == KeyEvent.KEYCODE_BUTTON_14
                || keyCode == KeyEvent.KEYCODE_BUTTON_15
                || keyCode == KeyEvent.KEYCODE_BUTTON_16;
    }

    private boolean handleKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int source = event.getSource();
        if (isGamepadKey(keyCode, source)) {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            try {
                onNativeGamepadKey(keyCode, down);
            } catch (Throwable t) {
                Log.w(TAG, "Failed onNativeGamepadKey", t);
            }
            return true;
        }
        return false;
    }

    private boolean handleGenericMotion(MotionEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
                || (source & InputDevice.SOURCE_JOYSTICK) != 0
                || (source & InputDevice.SOURCE_GAMEPAD) != 0) {

            // Left Stick
            float lx = event.getAxisValue(MotionEvent.AXIS_X);
            float ly = event.getAxisValue(MotionEvent.AXIS_Y);

            // Right Stick: Xbox wireless controllers often map to AXIS_Z / AXIS_RZ, or AXIS_RX / AXIS_RY
            float rx = event.getAxisValue(MotionEvent.AXIS_Z);
            float ry = event.getAxisValue(MotionEvent.AXIS_RZ);
            if (Math.abs(rx) < 0.001f && Math.abs(ry) < 0.001f) {
                float altRx = event.getAxisValue(MotionEvent.AXIS_RX);
                float altRy = event.getAxisValue(MotionEvent.AXIS_RY);
                if (Math.abs(altRx) > 0.001f || Math.abs(altRy) > 0.001f) {
                    rx = altRx;
                    ry = altRy;
                }
            }

            // Triggers: AXIS_LTRIGGER / AXIS_BRAKE / AXIS_Z, AXIS_RTRIGGER / AXIS_GAS / AXIS_RZ
            float lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
            if (lt == 0.0f) lt = event.getAxisValue(MotionEvent.AXIS_BRAKE);

            float rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
            if (rt == 0.0f) rt = event.getAxisValue(MotionEvent.AXIS_GAS);

            // D-Pad Hat
            float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

            try {
                onNativeGamepadAxis(lx, ly, rx, ry, lt, rt, hatX, hatY);
            } catch (Throwable t) {
                Log.w(TAG, "Failed onNativeGamepadAxis", t);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (handleKeyEvent(event)) {
            // Also let super dispatch so native activity knows about it
            super.dispatchKeyEvent(event);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (handleKeyEvent(event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (handleKeyEvent(event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (handleGenericMotion(event)) {
            super.dispatchGenericMotionEvent(event);
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (handleGenericMotion(event)) {
            return true;
        }
        return super.onGenericMotionEvent(event);
    }
}
