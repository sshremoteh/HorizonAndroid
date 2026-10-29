//! Android application context and lifecycle glue.
#[cfg(target_os = "android")]
use std::sync::OnceLock;
#[cfg(target_os = "android")]
pub use winit::platform::android::activity::AndroidApp;

#[cfg(target_os = "android")]
static APP: OnceLock<AndroidApp> = OnceLock::new();

#[cfg(target_os = "android")]
pub fn set_app(app: AndroidApp) {
    let _ = APP.set(app);
}

#[cfg(target_os = "android")]
pub fn get_app() -> Option<AndroidApp> {
    APP.get().cloned()
}

#[cfg(target_os = "android")]
pub fn internal_data_path() -> Option<std::path::PathBuf> {
    APP.get().and_then(|app| app.internal_data_path())
}

#[cfg(target_os = "android")]
pub fn external_data_path() -> Option<std::path::PathBuf> {
    APP.get().and_then(|app| app.external_data_path())
}

