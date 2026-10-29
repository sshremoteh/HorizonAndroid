//! Mobile touch controls and gesture recognition for Android / iOS.
//!
//! Provides virtual joystick calculation, camera look delta tracking,
//! and virtual action buttons (jump, sneak, attack, use, hotbar selection).

use std::collections::HashMap;

/// Identifier for a touch pointer.
pub type TouchId = u64;

#[derive(Clone, Copy, Debug, PartialEq)]
pub enum TouchRole {
    Joystick { origin_x: f32, origin_y: f32 },
    Look,
    JumpButton,
    SneakButton,
    HotbarSlot(u8),
}

#[derive(Clone, Copy, Debug)]
struct ActiveTouch {
    x: f32,
    y: f32,
    start_x: f32,
    start_y: f32,
    role: TouchRole,
}

/// The state of on-screen touch controls.
#[derive(Default)]
pub struct TouchControls {
    touches: HashMap<TouchId, ActiveTouch>,
    /// Accumulated camera rotation delta `[yaw_delta, pitch_delta]`.
    look_delta: [f32; 2],
    /// Current virtual movement direction `[strafe, forward]`.
    movement: [f32; 2],
    jump_down: bool,
    sneak_down: bool,
    attack_down: bool,
    use_down: bool,
    selected_hotbar: Option<u8>,
}

impl TouchControls {
    pub fn new() -> Self {
        Self::default()
    }

    /// Process a touch-down event.
    pub fn on_touch_down(&mut self, id: TouchId, x: f32, y: f32, width: f32, height: f32) {
        let role = self.classify_touch(x, y, width, height);
        self.touches.insert(
            id,
            ActiveTouch {
                x,
                y,
                start_x: x,
                start_y: y,
                role,
            },
        );

        match role {
            TouchRole::JumpButton => self.jump_down = true,
            TouchRole::SneakButton => self.sneak_down = !self.sneak_down,
            TouchRole::HotbarSlot(slot) => self.selected_hotbar = Some(slot),
            TouchRole::Joystick { .. } | TouchRole::Look => {}
        }
    }

    /// Process a touch-move event.
    pub fn on_touch_move(&mut self, id: TouchId, x: f32, y: f32) {
        let Some(touch) = self.touches.get_mut(&id) else {
            return;
        };
        let dx = x - touch.x;
        let dy = y - touch.y;
        touch.x = x;
        touch.y = y;

        match touch.role {
            TouchRole::Joystick { origin_x, origin_y } => {
                let jx = x - origin_x;
                let jy = y - origin_y;
                let radius = 64.0f32;
                let dist = (jx * jx + jy * jy).sqrt();
                if dist > 0.0 {
                    let clamped = dist.min(radius);
                    let nx = (jx / dist) * (clamped / radius);
                    let ny = (jy / dist) * (clamped / radius);
                    // Movement vector: index 0 is left/right (strafe), index 1 is forward/backward
                    self.movement = [nx, -ny];
                } else {
                    self.movement = [0.0, 0.0];
                }
            }
            TouchRole::Look => {
                // Sensitivity scaling
                const SENSITIVITY: f32 = 0.15;
                self.look_delta[0] += dx * SENSITIVITY;
                self.look_delta[1] += dy * SENSITIVITY;
            }
            _ => {}
        }
    }

    /// Process a touch-up event.
    pub fn on_touch_up(&mut self, id: TouchId) {
        if let Some(touch) = self.touches.remove(&id) {
            match touch.role {
                TouchRole::Joystick { .. } => {
                    self.movement = [0.0, 0.0];
                }
                TouchRole::JumpButton => {
                    self.jump_down = false;
                }
                TouchRole::Look => {
                    // Tap detection for interaction/attack if little movement
                    let dx = touch.x - touch.start_x;
                    let dy = touch.y - touch.start_y;
                    if dx * dx + dy * dy < 16.0 {
                        self.attack_down = true;
                    }
                }
                _ => {}
            }
        }
    }

    /// Consume the accumulated camera look delta.
    pub fn take_look_delta(&mut self) -> [f32; 2] {
        std::mem::take(&mut self.look_delta)
    }

    /// Movement direction vector `[strafe, forward]`.
    pub fn movement(&self) -> [f32; 2] {
        self.movement
    }

    pub fn is_jump_down(&self) -> bool {
        self.jump_down
    }

    pub fn is_sneak_down(&self) -> bool {
        self.sneak_down
    }

    pub fn take_attack(&mut self) -> bool {
        std::mem::take(&mut self.attack_down)
    }

    pub fn take_use(&mut self) -> bool {
        std::mem::take(&mut self.use_down)
    }

    pub fn take_hotbar_selection(&mut self) -> Option<u8> {
        self.selected_hotbar.take()
    }

    /// Classify a screen point into a touch control role.
    fn classify_touch(&self, x: f32, y: f32, width: f32, height: f32) -> TouchRole {
        let half_w = width * 0.5;

        // Bottom center: hotbar (9 slots across 50% screen width at bottom)
        let hotbar_y = height - 80.0;
        let hotbar_left = width * 0.25;
        let hotbar_right = width * 0.75;
        if y >= hotbar_y && x >= hotbar_left && x <= hotbar_right {
            let slot_w = (hotbar_right - hotbar_left) / 9.0;
            let slot = ((x - hotbar_left) / slot_w).clamp(0.0, 8.0) as u8;
            return TouchRole::HotbarSlot(slot);
        }

        // Bottom right corner: Jump button
        let jump_x = width - 120.0;
        let jump_y = height - 120.0;
        if x >= jump_x && y >= jump_y {
            return TouchRole::JumpButton;
        }

        // Left half: Virtual Joystick
        if x < half_w {
            TouchRole::Joystick {
                origin_x: x,
                origin_y: y,
            }
        } else {
            // Right half: Look & Interaction zone
            TouchRole::Look
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn touch_joystick_calculates_directional_movement() {
        let mut controls = TouchControls::new();
        let (w, h) = (1920.0, 1080.0);

        // Touch down at (200, 800) in left half
        controls.on_touch_down(1, 200.0, 800.0, w, h);
        assert_eq!(controls.movement(), [0.0, 0.0]);

        // Drag forward (upward in screen Y)
        controls.on_touch_move(1, 200.0, 750.0);
        let mov = controls.movement();
        assert!(mov[1] > 0.5, "Forward movement expected");
        assert_eq!(mov[0], 0.0, "No strafe expected");

        // Release
        controls.on_touch_up(1);
        assert_eq!(controls.movement(), [0.0, 0.0]);
    }

    #[test]
    fn touch_look_accumulates_and_drains_delta() {
        let mut controls = TouchControls::new();
        let (w, h) = (1920.0, 1080.0);

        // Touch down in right half
        controls.on_touch_down(2, 1200.0, 500.0, w, h);
        controls.on_touch_move(2, 1220.0, 510.0);

        let delta = controls.take_look_delta();
        assert!(delta[0] > 0.0);
        assert!(delta[1] > 0.0);

        // Drained
        assert_eq!(controls.take_look_delta(), [0.0, 0.0]);
    }

    #[test]
    fn jump_button_activates_in_corner() {
        let mut controls = TouchControls::new();
        let (w, h) = (1920.0, 1080.0);

        controls.on_touch_down(3, 1850.0, 1000.0, w, h);
        assert!(controls.is_jump_down());

        controls.on_touch_up(3);
        assert!(!controls.is_jump_down());
    }
}
