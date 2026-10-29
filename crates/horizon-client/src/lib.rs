//! Horizon native client: resources, world streaming, rendering and the
//! bindings between the validated simulation pieces and the game loop.
#![cfg_attr(not(target_os = "android"), forbid(unsafe_code))]
#![cfg_attr(target_os = "android", deny(unsafe_code))]

extern crate self as horizon_client;

pub mod advancements;
pub mod ambience;
pub mod animate_tick;
pub mod arrows;
pub mod assets;
pub mod atlas_sources;
pub mod attack_strength;
pub mod attribute_updates;
pub mod audio;
pub mod auth;
pub mod auto_jump;
pub mod banner;
pub mod beehive;
pub mod benchmark;
pub mod bidi;
pub mod biome;
pub mod block_entities;
pub mod block_entity_anim;
pub mod block_entity_item;
pub mod block_entity_render;
pub mod block_transform;
pub mod border_render;
pub mod boss_overlay;
pub mod breaking;
pub mod brewing;
pub mod camera;
pub mod cauldron;
pub mod chat;
pub mod chunk;
pub mod chunk_map;
pub mod clock;
pub mod clouds;
pub mod collisions;
pub mod command_block;
pub mod command_storage;
pub mod confirm_screen;
pub mod container_screen;
pub mod containers;
pub mod control;
pub mod crafting;
pub mod crash;
pub mod creative;
pub mod cushion;
pub mod data_cmd;
pub mod datapack;
pub mod debug_chart;
pub mod debug_keys;
pub mod debug_overlay;
pub mod dialog;
pub mod dialog_screen;
pub mod discord_presence;
pub mod dynamic_light;
pub mod effects;
pub mod end_crystal;
pub mod entities;
pub mod entity_culling;
pub mod entity_nbt;
pub mod environment;
pub mod extras;
pub mod extras_hud;
pub mod falling_block;
pub mod fast_hash;
pub mod fire_render;
pub mod first_person;
pub mod flatgen;
pub mod fog;
pub mod font;
pub mod friends_screen;
pub mod furnace;
pub mod game;
pub mod game_mode_switcher;
pub mod game_rules_screen;
pub mod gfx;
pub mod gizmos;
pub mod gossip;
pub mod graphics;
pub mod graphics_style;
pub mod graphics_ui;
pub mod health;
pub mod heartbeat;
pub mod hotbar;
pub mod hud;
pub mod hud_layout;
pub mod hud_overlays;
pub mod hunger;
pub mod inv_sort;
pub mod inventory_effects;
pub mod item_render;
pub mod item_sync;
pub mod item_tooltip;
pub mod java_text;
pub mod lan;
pub mod landing;
pub mod level_loading;
pub mod light;
pub mod locator_bar;
pub mod lod;
pub mod logging;
pub mod menu;
pub mod menu_blur;
pub mod merchant;
pub mod mesher;
pub mod minimap;
pub mod mipmap;
pub mod mobs;
pub mod mod_content;
pub mod mod_list;
mod model_library;
pub mod models;
pub mod motion;
pub mod moved_extras;
pub mod multiplayer;
pub mod music;
pub mod net;
pub mod nether_portal;
pub mod offline_name;
pub mod options_file;
pub mod options_fixes;
pub mod options_ui;
pub mod osk;
pub mod overlay;
pub mod pack_screen;
pub mod packs;
pub mod par;
pub mod particles;
pub mod patrol;
pub mod perf_overlay;
pub mod pick_item;
pub mod player_sync;
pub mod poi_sections;
pub mod portal_sections;
pub mod pose;
pub mod pregen;
pub mod projectiles;
pub mod providers;
pub mod raid;
pub mod random_tick_sections;
pub mod recipe_book;
pub mod recipe_book_screen;
pub mod reconnect;
pub mod save;
pub mod scoreboard_cmd;
pub mod screen_draw;
pub mod screen_effects;
pub mod screen_layout;
pub mod sculk;
pub use horizon_server_files as server;
pub mod server_packs;
pub mod settings;
pub mod shader_pack_list;
pub mod shadows;
pub mod signs;
pub mod skins;
pub mod sound_engine;
pub mod sound_events;
pub mod spans;
pub mod spear;
pub mod spectator_menu;
pub mod sprint;
pub mod sprites;
pub mod stations;
pub mod stopwatches;
pub mod streaming;
pub mod structure_blocks;
pub mod tab_list;
pub mod telemetry;
pub mod testgen;
pub mod text_display;
pub mod tick_chunk_membership;
pub mod ticket_levels;
pub mod title;
pub mod title_edition;
pub mod toasts;
pub mod trims;
pub mod ui;
pub mod ui_theme;
pub mod vanillagen;
pub mod vault;
pub mod weather;
pub mod widgets;
pub mod win_screen;
pub mod world;
pub mod world_spawn;
pub mod worldborder;
pub mod zip;
pub use horizon_client_core::Result;
pub use horizon_client_core::block_entity_nbt;
pub use horizon_client_core::bossbar;
pub use horizon_client_core::component;
pub use horizon_client_core::fastutil_order;
pub use horizon_client_core::gamerules;
pub use horizon_client_core::lang;
pub use horizon_client_core::nbt_path;
pub use horizon_client_core::scoreboard;
pub use horizon_client_core::snbt;
pub use horizon_client_core::text;
pub use horizon_client_input::clipboard;
pub use horizon_client_input::gamepad;
pub use horizon_client_input::input;
pub use horizon_client_input::interaction_input;
pub use horizon_client_input::key_names;
pub use horizon_client_input::keybinds;
pub use horizon_client_input::keymapping;
pub use horizon_client_input::mouse;
pub use horizon_client_input::window;
pub use horizon_command as command;
pub use horizon_items::blocks;
pub use horizon_items::bundle;
pub use horizon_items::cooldowns;
pub use horizon_items::enchant;
pub use horizon_items::item_data;
pub use horizon_items::items;
pub use horizon_items::random_sequences;

#[cfg(target_os = "android")]
pub mod android_app;

#[cfg(target_os = "android")]
#[allow(unsafe_code)]
unsafe extern "C" {
    fn __android_log_print(
        prio: std::ffi::c_int,
        tag: *const std::ffi::c_char,
        fmt: *const std::ffi::c_char,
        ...
    ) -> std::ffi::c_int;
}

#[cfg(target_os = "android")]
#[allow(unsafe_code)]
pub fn log_android(prio: i32, msg: &str) {
    let tag = std::ffi::CString::new("Horizon").unwrap_or_default();
    let fmt = std::ffi::CString::new("%s").unwrap_or_default();
    let text = std::ffi::CString::new(msg).unwrap_or_default();
    unsafe {
        __android_log_print(prio, tag.as_ptr(), fmt.as_ptr(), text.as_ptr());
    }
}

#[cfg(target_os = "android")]
pub fn write_crash_report(msg: &str) {
    log_android(6, msg);
    let mut targets = Vec::new();
    if let Some(ext) = android_app::external_data_path() {
        let _ = std::fs::create_dir_all(&ext);
        targets.push(ext.join("crash.txt"));
    }
    if let Some(int) = android_app::internal_data_path() {
        let _ = std::fs::create_dir_all(&int);
        targets.push(int.join("crash.txt"));
    }
    targets.push(std::path::PathBuf::from("/sdcard/horizon/crash.txt"));
    targets.push(std::path::PathBuf::from("/sdcard/horizon-crash.txt"));

    for path in targets {
        if let Some(parent) = path.parent() {
            let _ = std::fs::create_dir_all(parent);
        }
        let _ = std::fs::write(&path, msg);
    }
}

#[cfg(target_os = "android")]
#[allow(unsafe_code)]
#[unsafe(no_mangle)]
pub fn android_main(app: winit::platform::android::activity::AndroidApp) {
    android_app::set_app(app);

    std::panic::set_hook(Box::new(|info| {
        let backtrace = std::backtrace::Backtrace::capture();
        let crash_msg = format!("CRITICAL PANIC in Horizon Android:\n{}\nBacktrace:\n{}\n", info, backtrace);
        write_crash_report(&crash_msg);
    }));

    // Find the primary working directory (must be writable)
    let app_ext = android_app::external_data_path();
    let sdcard_home = std::path::PathBuf::from("/sdcard/horizon");
    let home = if let Some(ref ext) = app_ext {
        let _ = std::fs::create_dir_all(ext);
        ext.clone()
    } else if std::fs::create_dir_all(&sdcard_home).is_ok() {
        sdcard_home
    } else {
        android_app::internal_data_path()
            .unwrap_or_else(|| std::path::PathBuf::from("/data/local/tmp"))
    };

    let _ = std::fs::create_dir_all(&home);
    let _ = std::env::set_current_dir(&home);
    logging::init(&home, "Render thread");

    let startup_msg = format!(
        "Horizon Android starting up from android_main. Home: {}, Internal: {:?}, External: {:?}",
        home.display(),
        android_app::internal_data_path(),
        android_app::external_data_path()
    );
    log_android(4, &startup_msg);
    log::info!("{}", startup_msg);

    if let Err(e) = game::run() {
        let err_msg = format!("Horizon Android Game Error:\n{}\n", e);
        write_crash_report(&err_msg);
    }
}
