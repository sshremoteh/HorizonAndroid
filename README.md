<p align="center">
  <img src="docs/media/banner.webp" alt="Horizon Android: Minecraft Java Edition 26.3 in Native Rust on Android" width="100%">
</p>

<p align="center">
  <a href="https://github.com/sshremoteh/HorizonAndroid/releases/latest"><img src="https://img.shields.io/badge/Release-v0.1.2--android-5b3fd6?style=for-the-badge&logo=android&logoColor=white" alt="Latest Release"></a>
  <img src="https://img.shields.io/badge/Minecraft-Java%2026.3-5D9C3F?style=for-the-badge" alt="Minecraft Java 26.3">
  <img src="https://img.shields.io/badge/Architecture-ARM64%20(aarch64)-orange?style=for-the-badge&logo=arm" alt="ARM64">
  <img src="https://img.shields.io/badge/Graphics-Vulkan%201.1+-red?style=for-the-badge&logo=vulkan&logoColor=white" alt="Vulkan">
  <img src="https://img.shields.io/badge/Language-Rust%20100%25-B7410E?style=for-the-badge&logo=rust&logoColor=white" alt="Rust">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-3B2F7E?style=for-the-badge" alt="MIT License"></a>
</p>

<p align="center">
  <a href="#-quick-download"><b>Быстрый старт</b></a> •
  <a href="#-особенности-порта"><b>Особенности</b></a> •
  <a href="#-сенсорное-управление-и-редактор"><b>Сенсорное управление</b></a> •
  <a href="#-инструкция-по-установке"><b>Установка</b></a> •
  <a href="#-сборка-из-исходников"><b>Сборка</b></a> •
  <a href="https://github.com/sshremoteh/HorizonAndroid/releases"><b>Релизы</b></a>
</p>

---

## 📱 О проекте Horizon Android

**Horizon Android** — нативный порт проекта [Horizon](https://github.com/horizon-rust/horizon) (Minecraft: Java Edition 26.3, полностью переписанного на Rust) для мобильных устройств под управлением **Android**.

В отличие от эмуляторов и Java-оболочек (PojavLauncher, Zalith), здесь **нет виртуальной машины Java (JVM)** в рантайме игры. Клиент компилируется непосредственно в машинный код **`aarch64`** с прямым рендерингом через **Vulkan (wgpu)** и вызовом нативного API Android (`android_native_app_glue`).

> ⚠️ **Важно**: Это неофициальный открытый проект. Он не связан с Mojang или Microsoft. Для игры требуются ресурсы вашей легально приобретённой копии Minecraft: Java Edition 26.3.

---

## 🚀 Быстрый старт / Quick Download

Все готовые сборки доступны в [GitHub Releases](https://github.com/sshremoteh/HorizonAndroid/releases/latest):

| Файл | Описание | Ссылка |
|---|---|---|
| 📦 **`horizon-android.apk`** | Установочный пакет Android (ARM64, Android 8.0+) | [Скачать APK](https://github.com/sshremoteh/HorizonAndroid/releases/latest/download/horizon-android.apk) |
| 🗂️ **`horizon.zip`** | Готовый архив игровых ресурсов и ассетов 26.3 | [Скачать ZIP](https://github.com/sshremoteh/HorizonAndroid/releases/latest/download/horizon.zip) |

---

## ✨ Особенности порта

- ⚡ **100% Native Rust & Vulkan**: минимальное потребление оперативной памяти, мгновенный старт, холодный запуск без джиттера сборщика мусора.
- 🎮 **Сенсорное управление в стиле Zalith / Pojav**:
  - Классический **D-Pad со стрелками** (`^`, `v`, `<`, `>`) и кнопкой приседания (`SNK`) по центру.
  - Сенсорные кнопки основных действий: `JUMP` (прыжок), `RUN` (спринт-переключатель), `PRI` (атака / ЛКМ), `SEC` (действие / ПКМ), `DROP` (выбросить / Q).
  - Верхняя панель: `ESC` (пауза / меню), `F3` (отладка), `F5` (вид камеры), `CHAT` (чат), `TAB` (список игроков), `INV` (инвентарь / E), `⚙ EDIT` (редактор).
  - Свайп по правой половине экрана — плавное вращение камеры.
  - Нажатия на нижнюю панель — быстрый выбор слота хотбара.
- ⚙️ **Внутриигровой визуальный редактор оверлея**:
  - Нажмите **`⚙ EDIT`**, чтобы войти в режим настройки.
  - **Перемещение**: просто удерживайте и перетаскивайте любую кнопку пальцем.
  - **Добавление кнопок**: создавайте новые кнопки на лету (`+ Кнопка`).
  - **Размер**: меняйте размер кнопок кнопками `+ Разм` и `- Разм`.
  - **Назначение любых клавиш**: сетка выбора клавиш (`W`, `A`, `S`, `D`, `Space`, `Shift`, `Ctrl`, `Esc`, `E`, `Q`, `T`, `Tab`, `F1`–`F5`, `F11`, `LMB`, `RMB`, `1`–`9`).
  - Конфигурация сохраняется в удобном JSON-формате: `/sdcard/horizon/touch_layout.json`.
- 🕹️ **Поддержка геймпадов**:
  - Полная поддержка геймпадов Xbox, PlayStation DualSense и стандартных Bluetooth-контроллеров (ходьба стиками, вращение камеры правым стиком, триггеры атаки/строительства, прыжок на A/Cross).
- 🌍 **100% Vanilla Parity**:
  - Миры и сиды полностью идентичны Minecraft Java Edition 26.3.
  - Загрузка существующих сохранений Java Edition.
  - Полная физика мобов, механизмов редстоуна, редких биомов и пещер.

---

## 📸 Скриншоты

<table>
  <tr>
    <td width="50%"><img src="docs/media/survival.webp" alt="Выживание на закате"></td>
    <td width="50%"><img src="docs/media/village.webp" alt="Деревня жителей в саванне"></td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/media/nether.webp" alt="Багровый лес в Нижнем мире"></td>
    <td width="50%"><img src="docs/media/sunset.webp" alt="Генерация ландшафта на движке wgpu"></td>
  </tr>
</table>

---

## 🛠️ Инструкция по установке

### Шаг 1. Установка APK
1. Загрузите [**`horizon-android.apk`**](https://github.com/sshremoteh/HorizonAndroid/releases/latest/download/horizon-android.apk).
2. Разрешите установку из неизвестных источников и установите приложение.
3. При первом запуске предоставьте разрешение на доступ к хранилищу файлов (нужно для загрузки ресурсов и сохранения миров).

### Шаг 2. Размещение ресурсов игры
1. Загрузите [**`horizon.zip`**](https://github.com/sshremoteh/HorizonAndroid/releases/latest/download/horizon.zip).
2. Распакуйте архив в корень внутреннего накопителя устройства так, чтобы путь к ресурсам выглядел следующим образом:
```text
/sdcard/horizon/
├── resources/              <-- Папка с распакованными файлами из horizon.zip
│   ├── blocks/
│   ├── items/
│   ├── models/
│   ├── textures/
│   └── ...
├── touch_layout.json       <-- Создается автоматически (настройки кнопок)
└── saves/                  <-- Ваши миры Minecraft Java
```
3. Запустите **Horizon** с рабочего стола телефона или планшета!

---

## 🎮 Настройка управления (Touch Overlay Editor)

В правом верхнем углу экрана расположена кнопка **`⚙`**:

```text
[+ Кнопка]  [+ Разм]  [- Разм]  [Клавиша]  [Удалить]  [Сброс]  ...  [Готово]
```

1. **Выбор кнопки**: нажмите на любую кнопку оверлея — она подсветится бирюзовой рамкой.
2. **Перемещение**: ведите пальцем по экрану, чтобы перетащить кнопку в любое удобное место.
3. **Размер**: нажимайте `+ Разм` или `- Разм` на верхней панели.
4. **Смена клавиши**: нажмите `Клавиша` — откроется сетка доступных кнопок клавиатуры и мыши (включая F1–F5, ESC, CTRL, инвентарь E, цифры слотов 1–9).
5. **Сохранение**: нажмите `Готово` — новая раскладка сразу применится и запишется в `/sdcard/horizon/touch_layout.json`.

---

## 💻 Сборка из исходников

### Требования
- Linux (Ubuntu 22.04+ / Debian 12+)
- Rust 1.90+ с установленной целью: `rustup target add aarch64-linux-android`
- Android NDK (r25c+ или r27+) и Android SDK (API 34, `aapt2`, `d8`, `apksigner`, `zipalign`)
- Java JDK 17+

### Команды сборки:
```bash
git clone https://github.com/sshremoteh/HorizonAndroid.git
cd HorizonAndroid/horizon

# Быстрая сборка APK (Fast Profile)
bash packaging/android/build_apk.sh fast

# Релизная оптимизированная сборка
bash packaging/android/build_apk.sh release
```
Готовый подписанный APK появится в `packaging/android/horizon-android-debug.apk`.

---

## 🗺️ Структура репозитория

| Папка / Файл | Назначение |
|---|---|
| `crates/horizon-client/src/touch_controls.rs` | Сенсорный оверлей, D-Pad, система кастомизации и сохранение JSON |
| `crates/horizon-client/src/game.rs` | Игровой цикл, обработка касаний, рендеринг HUD и меню |
| `packaging/android/` | AndroidManifest, NativeActivity, скрипт сборки `build_apk.sh` |
| `docs/design-system.md` | Спецификация дизайн-системы Horizon |
| `crates/horizon-world/` | Логика мира Minecraft (генерация, чанки, физика, редстоун) |

---

## 📄 Лицензия

Horizon распространяется под лицензией **MIT**. См. файл [LICENSE](LICENSE).  
Minecraft является зарегистрированным товарным знаком компании Mojang Synergies AB.
