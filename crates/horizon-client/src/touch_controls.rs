//! Customizable on-screen touch controls layout and overlay editor (Zalith / Pojav style).

use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::fs;
use std::path::{Path, PathBuf};
use winit::keyboard::KeyCode;

pub const AVAILABLE_KEYS: &[(&str, &str)] = &[
    ("w", "W"),
    ("s", "S"),
    ("a", "A"),
    ("d", "D"),
    ("space", "JUMP"),
    ("shift", "SNK"),
    ("ctrl", "RUN"),
    ("esc", "ESC"),
    ("e", "INV"),
    ("q", "DROP"),
    ("t", "CHAT"),
    ("tab", "TAB"),
    ("f1", "F1"),
    ("f2", "F2"),
    ("f3", "F3"),
    ("f4", "F4"),
    ("f5", "F5"),
    ("f11", "F11"),
    ("lmb", "PRI"),
    ("rmb", "SEC"),
    ("1", "1"),
    ("2", "2"),
    ("3", "3"),
    ("4", "4"),
    ("5", "5"),
    ("6", "6"),
    ("7", "7"),
    ("8", "8"),
    ("9", "9"),
];

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct TouchButton {
    pub id: String,
    pub label: String,
    pub key: String,
    /// X center/position normalized to screen width (0.0..1.0)
    pub x: f32,
    /// Y center/position normalized to screen height (0.0..1.0)
    pub y: f32,
    /// Width normalized relative to screen height (so aspect ratio is preserved)
    pub w: f32,
    /// Height normalized relative to screen height
    pub h: f32,
    #[serde(default)]
    pub toggle: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct TouchLayout {
    pub buttons: Vec<TouchButton>,
    #[serde(default = "default_opacity")]
    pub opacity: u8,
}

fn default_opacity() -> u8 {
    160
}

impl Default for TouchLayout {
    fn default() -> Self {
        Self {
            opacity: 160,
            buttons: vec![
                // --- Left Arrow D-Pad ---
                TouchButton {
                    id: "btn_up".into(),
                    label: "^".into(),
                    key: "w".into(),
                    x: 0.11,
                    y: 0.62,
                    w: 0.14,
                    h: 0.14,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_down".into(),
                    label: "v".into(),
                    key: "s".into(),
                    x: 0.11,
                    y: 0.84,
                    w: 0.14,
                    h: 0.14,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_left".into(),
                    label: "<".into(),
                    key: "a".into(),
                    x: 0.04,
                    y: 0.73,
                    w: 0.14,
                    h: 0.14,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_right".into(),
                    label: ">".into(),
                    key: "d".into(),
                    x: 0.18,
                    y: 0.73,
                    w: 0.14,
                    h: 0.14,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_snk".into(),
                    label: "SNK".into(),
                    key: "shift".into(),
                    x: 0.11,
                    y: 0.73,
                    w: 0.14,
                    h: 0.14,
                    toggle: true,
                },
                // --- Right Action Buttons ---
                TouchButton {
                    id: "btn_jump".into(),
                    label: "JUMP".into(),
                    key: "space".into(),
                    x: 0.88,
                    y: 0.73,
                    w: 0.18,
                    h: 0.18,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_run".into(),
                    label: "RUN".into(),
                    key: "ctrl".into(),
                    x: 0.88,
                    y: 0.52,
                    w: 0.14,
                    h: 0.14,
                    toggle: true,
                },
                TouchButton {
                    id: "btn_pri".into(),
                    label: "PRI".into(),
                    key: "lmb".into(),
                    x: 0.77,
                    y: 0.52,
                    w: 0.14,
                    h: 0.14,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_sec".into(),
                    label: "SEC".into(),
                    key: "rmb".into(),
                    x: 0.77,
                    y: 0.73,
                    w: 0.14,
                    h: 0.14,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_drop".into(),
                    label: "DROP".into(),
                    key: "q".into(),
                    x: 0.66,
                    y: 0.73,
                    w: 0.13,
                    h: 0.13,
                    toggle: false,
                },
                // --- Top / Utility Bar (Zalith Style) ---
                TouchButton {
                    id: "btn_esc".into(),
                    label: "ESC".into(),
                    key: "esc".into(),
                    x: 0.03,
                    y: 0.05,
                    w: 0.13,
                    h: 0.08,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_f3".into(),
                    label: "F3".into(),
                    key: "f3".into(),
                    x: 0.14,
                    y: 0.05,
                    w: 0.11,
                    h: 0.08,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_f5".into(),
                    label: "F5".into(),
                    key: "f5".into(),
                    x: 0.23,
                    y: 0.05,
                    w: 0.11,
                    h: 0.08,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_chat".into(),
                    label: "CHAT".into(),
                    key: "t".into(),
                    x: 0.33,
                    y: 0.05,
                    w: 0.13,
                    h: 0.08,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_tab".into(),
                    label: "TAB".into(),
                    key: "tab".into(),
                    x: 0.44,
                    y: 0.05,
                    w: 0.12,
                    h: 0.08,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_inv".into(),
                    label: "INV".into(),
                    key: "e".into(),
                    x: 0.78,
                    y: 0.05,
                    w: 0.13,
                    h: 0.08,
                    toggle: false,
                },
                TouchButton {
                    id: "btn_edit".into(),
                    label: "⚙".into(),
                    key: "edit".into(),
                    x: 0.89,
                    y: 0.05,
                    w: 0.11,
                    h: 0.08,
                    toggle: false,
                },
            ],
        }
    }
}

impl TouchLayout {
    pub fn config_path() -> PathBuf {
        let external = Path::new("/sdcard/horizon/touch_layout.json");
        if external.parent().is_some_and(|p| p.exists()) {
            return external.to_path_buf();
        }
        PathBuf::from("touch_layout.json")
    }

    pub fn load_or_default() -> Self {
        let path = Self::config_path();
        if path.exists() {
            if let Ok(content) = fs::read_to_string(&path) {
                if let Ok(layout) = serde_json::from_str::<Self>(&content) {
                    return layout;
                }
            }
        }
        let def = Self::default();
        let _ = def.save();
        def
    }

    pub fn save(&self) -> Result<(), String> {
        let path = Self::config_path();
        if let Some(parent) = path.parent() {
            let _ = fs::create_dir_all(parent);
        }
        let json = serde_json::to_string_pretty(self).map_err(|e| e.to_string())?;
        fs::write(path, json).map_err(|e| e.to_string())?;
        Ok(())
    }

    /// Converts normalized button rect into GUI coordinates (gx, gy, gw, gh).
    pub fn button_rect(&self, btn: &TouchButton, gw: f32, gh: f32) -> (f32, f32, f32, f32) {
        let w = btn.w * gh;
        let h = btn.h * gh;
        let x = (btn.x * gw) - (w / 2.0);
        let y = (btn.y * gh) - (h / 2.0);
        (x.clamp(0.0, gw - w), y.clamp(0.0, gh - h), w, h)
    }

    /// Finds index of button under point (gx, gy).
    pub fn find_button(&self, gx: f32, gy: f32, gw: f32, gh: f32) -> Option<usize> {
        for (i, btn) in self.buttons.iter().enumerate().rev() {
            let (bx, by, bw, bh) = self.button_rect(btn, gw, gh);
            if gx >= bx && gx <= bx + bw && gy >= by && gy <= by + bh {
                return Some(i);
            }
        }
        None
    }
}

pub fn keycode_from_name(name: &str) -> Option<KeyCode> {
    match name {
        "w" => Some(KeyCode::KeyW),
        "s" => Some(KeyCode::KeyS),
        "a" => Some(KeyCode::KeyA),
        "d" => Some(KeyCode::KeyD),
        "space" => Some(KeyCode::Space),
        "shift" => Some(KeyCode::ShiftLeft),
        "ctrl" => Some(KeyCode::ControlLeft),
        "esc" => Some(KeyCode::Escape),
        "e" => Some(KeyCode::KeyE),
        "q" => Some(KeyCode::KeyQ),
        "t" => Some(KeyCode::KeyT),
        "tab" => Some(KeyCode::Tab),
        "f1" => Some(KeyCode::F1),
        "f2" => Some(KeyCode::F2),
        "f3" => Some(KeyCode::F3),
        "f4" => Some(KeyCode::F4),
        "f5" => Some(KeyCode::F5),
        "f11" => Some(KeyCode::F11),
        "1" => Some(KeyCode::Digit1),
        "2" => Some(KeyCode::Digit2),
        "3" => Some(KeyCode::Digit3),
        "4" => Some(KeyCode::Digit4),
        "5" => Some(KeyCode::Digit5),
        "6" => Some(KeyCode::Digit6),
        "7" => Some(KeyCode::Digit7),
        "8" => Some(KeyCode::Digit8),
        "9" => Some(KeyCode::Digit9),
        _ => None,
    }
}

/// Runtime state for touch overlay and editor.
pub struct TouchOverlayState {
    pub layout: TouchLayout,
    pub is_editing: bool,
    pub selected_index: Option<usize>,
    pub dragging_index: Option<usize>,
    pub drag_start_pointer: (f32, f32),
    pub drag_orig_pos: (f32, f32),
    pub key_picker_open: bool,
    pub active_toggles: HashSet<String>,
    pub active_touches: HashMap<u64, String>,
}

impl TouchOverlayState {
    pub fn new() -> Self {
        Self {
            layout: TouchLayout::load_or_default(),
            is_editing: false,
            selected_index: None,
            dragging_index: None,
            drag_start_pointer: (0.0, 0.0),
            drag_orig_pos: (0.0, 0.0),
            key_picker_open: false,
            active_toggles: HashSet::new(),
            active_touches: HashMap::new(),
        }
    }
}
