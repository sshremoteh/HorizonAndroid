#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TARGET="aarch64-linux-android"
PROFILE="${1:-fast}"
BUILD_MODE="${2:-rac}" # "rac" (ultra-fast standalone packager) or "aapt2" (classic android-sdk toolchain)
ANDROID_JAR="/opt/android-sdk/platforms/android-34/android.jar"
KEYSTORE="/opt/android-sdk/debug.keystore"
BUILD_DIR="${SCRIPT_DIR}/build"

echo "=== Building Horizon Android (Native Rust Client) [profile=${PROFILE}, packager=${BUILD_MODE}] ==="

# 1. Compile Rust native library
echo "-> Compiling libhorizon_client.so for ${TARGET} (profile=${PROFILE})..."
cd "${REPO_ROOT}"
source /root/.cargo/env
cargo build --target "${TARGET}" --lib --profile "${PROFILE}" -p horizon-client

# 2. Setup build directory
cd "${SCRIPT_DIR}"
rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}/lib/arm64-v8a"
mkdir -p "${BUILD_DIR}/java_bin"

# 3. Copy native shared library
echo "-> Packaging native shared libraries..."
cp "${REPO_ROOT}/target/${TARGET}/${PROFILE}/libhorizon_client.so" "${BUILD_DIR}/lib/arm64-v8a/"

# 4. Compile Java Activity and create classes.dex
echo "-> Compiling Java Activity classes..."
javac --release 17 -encoding UTF-8 -cp "${ANDROID_JAR}" \
    -d "${BUILD_DIR}/java_bin" \
    "${SCRIPT_DIR}"/java/dev/playhorizon/horizon/*.java

echo "-> Converting bytecode to classes.dex with D8..."
java -cp /opt/android-sdk/build-tools/r8.jar com.android.tools.r8.D8 \
    --min-api 26 \
    --output "${BUILD_DIR}" \
    $(find "${BUILD_DIR}/java_bin" -name "*.class")

if [ "${BUILD_MODE}" = "rac" ] && command -v rac >/dev/null 2>&1; then
    # --- Ultra-Fast RAC Packaging & V2 Signing Pipeline ---
    echo "-> Packaging and signing APK via RAC (Rust Android Compiler)..."
    cp "${SCRIPT_DIR}/AndroidManifest.xml" "${BUILD_DIR}/"
    cp -r "${SCRIPT_DIR}/res" "${BUILD_DIR}/"

    rac build "${BUILD_DIR}" -o "${SCRIPT_DIR}/horizon-android-debug.apk"
    rac verify "${SCRIPT_DIR}/horizon-android-debug.apk"
else
    # --- Classic aapt2 + zipalign + apksigner Pipeline ---
    echo "-> Compiling Android resources with aapt2..."
    aapt2 compile --dir res -o "${BUILD_DIR}/compiled_res.zip"

    echo "-> Linking APK with aapt2..."
    aapt2 link -I "${ANDROID_JAR}" \
        --manifest AndroidManifest.xml \
        -o "${BUILD_DIR}/unaligned.apk" \
        "${BUILD_DIR}/compiled_res.zip"

    echo "-> Adding native library and classes.dex to APK..."
    cd "${BUILD_DIR}"
    python3 -c '
import zipfile
with zipfile.ZipFile("unaligned.apk", "a") as apk:
    apk.write("classes.dex", "classes.dex")
    apk.write("lib/arm64-v8a/libhorizon_client.so", "lib/arm64-v8a/libhorizon_client.so", compress_type=zipfile.ZIP_STORED)
'
    echo "-> Zipaligning APK..."
    zipalign -f -p 4 "${BUILD_DIR}/unaligned.apk" "${BUILD_DIR}/horizon-aligned.apk"

    echo "-> Signing APK..."
    apksigner sign --ks "${KEYSTORE}" \
        --ks-pass pass:android \
        --out "${SCRIPT_DIR}/horizon-android-debug.apk" \
        "${BUILD_DIR}/horizon-aligned.apk"
fi

# 5. Copy to /sdcard
echo "-> Copying APK to /sdcard/horizon-android.apk..."
mkdir -p /sdcard/horizon/resources
cp -f "${SCRIPT_DIR}/horizon-android-debug.apk" "/sdcard/horizon-android.apk"

echo "=== Build Complete! ==="
echo "Output APK: ${SCRIPT_DIR}/horizon-android-debug.apk"
echo "SDCard APK: /sdcard/horizon-android.apk"
ls -lh /sdcard/horizon-android.apk
