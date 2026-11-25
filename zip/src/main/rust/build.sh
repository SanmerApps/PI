#!/usr/bin/env sh

build() {
    LIB_NAME="z_rs"
    TARGET="$1"
    OUTPUT_DIR="libs/$2"
    cargo build --profile release --target "$TARGET" || exit 1
    mkdir -p "$OUTPUT_DIR" && cp "target/$TARGET/release/lib${LIB_NAME}.so" "$OUTPUT_DIR/" || exit 1
}

cd -P "$(dirname $0)" || exit 1
build aarch64-linux-android arm64-v8a
build x86_64-linux-android x86_64