//! Android gamepad state and JNI hooks.
use std::sync::Mutex;
use horizon_client_input::gamepad::{Button, PadState};

static STATE: Mutex<PadState> = Mutex::new(PadState {
    buttons: 0,
    left: [0.0, 0.0],
    right: [0.0, 0.0],
    triggers: [0.0, 0.0],
});

/// Returns the latest controller state recorded from Android input.
pub fn current_state() -> PadState {
    *STATE.lock().unwrap()
}

/// Called when an Android key code event arrives from a gamepad or joystick.
pub fn on_key(key_code: i32, pressed: bool) {
    let mut state = STATE.lock().unwrap();
    if let Some(btn) = map_key_code(key_code) {
        state.set(btn, pressed);
    }
}

/// Called when Android joystick / gamepad motion axis values arrive.
pub fn on_axis(lx: f32, ly: f32, rx: f32, ry: f32, lt: f32, rt: f32, hat_x: f32, hat_y: f32) {
    let mut state = STATE.lock().unwrap();
    // Invert Y so up is positive
    state.left = [lx, -ly];
    state.right = [rx, -ry];
    state.triggers = [lt.clamp(0.0, 1.0), rt.clamp(0.0, 1.0)];

    // D-pad Hat axes
    state.set(Button::DPadUp, hat_y < -0.5);
    state.set(Button::DPadDown, hat_y > 0.5);
    state.set(Button::DPadLeft, hat_x < -0.5);
    state.set(Button::DPadRight, hat_x > 0.5);
}

fn map_key_code(code: i32) -> Option<Button> {
    match code {
        // A / Cross
        96 => Some(Button::South),
        // B / Circle
        97 => Some(Button::East),
        // X / Square
        99 => Some(Button::West),
        // Y / Triangle
        100 => Some(Button::North),
        // L1 / LB
        102 => Some(Button::LeftBumper),
        // R1 / RB
        103 => Some(Button::RightBumper),
        // L2 / LT
        104 => Some(Button::LeftTrigger),
        // R2 / RT
        105 => Some(Button::RightTrigger),
        // L3 / Left Stick
        106 => Some(Button::LeftStick),
        // R3 / Right Stick
        107 => Some(Button::RightStick),
        // Start / Menu
        108 => Some(Button::Start),
        // Select / Back
        109 => Some(Button::Select),
        // Dpad keys
        19 => Some(Button::DPadUp),
        20 => Some(Button::DPadDown),
        21 => Some(Button::DPadLeft),
        22 => Some(Button::DPadRight),
        _ => None,
    }
}

// JNI exports for HorizonGameActivity
#[allow(unsafe_code)]
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_dev_playhorizon_horizon_HorizonGameActivity_onNativeGamepadKey(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    key_code: i32,
    pressed: u8,
) {
    on_key(key_code, pressed != 0);
}

#[allow(unsafe_code)]
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_dev_playhorizon_horizon_HorizonGameActivity_onNativeGamepadAxis(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    lx: f32,
    ly: f32,
    rx: f32,
    ry: f32,
    lt: f32,
    rt: f32,
    hat_x: f32,
    hat_y: f32,
) {
    on_axis(lx, ly, rx, ry, lt, rt, hat_x, hat_y);
}
