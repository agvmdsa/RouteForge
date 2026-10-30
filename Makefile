# Builds RouteForge from the terminal, without Android Studio. `make` lists the targets.
#
#   make android            build the debug APK
#   make run                install it on the connected phone or emulator and open it
#   make android GRADLE_FLAGS=--offline     any extra Gradle option goes in GRADLE_FLAGS
#
# It calls the same Gradle tasks as .github/workflows/ci.yml, so a green `make check` is what the pull request will run.
# Written for the GNU Make 3.81 that macOS ships.

GRADLEW := ./gradlew
GRADLE_FLAGS ?=
APP_ID := com.routeforge.app
APK := app/build/outputs/apk/debug/app-debug.apk

# JDKs: any JDK on the PATH (or JAVA_HOME) is enough to start the Gradle wrapper. The build itself needs two installed: JDK 21 for
# the compiler (the toolchain in build-logic) and JDK 25 for the Gradle daemon (pinned in gradle/gradle-daemon-jvm.properties,
# which also holds the URLs Gradle downloads it from when it is missing).

# The SDK is found the way Gradle finds it: sdk.dir in local.properties, then ANDROID_HOME, then ANDROID_SDK_ROOT.
SDK_DIR := $(or $(shell sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null),$(ANDROID_HOME),$(ANDROID_SDK_ROOT))
ifndef ADB
ADB := $(or $(shell command -v adb 2>/dev/null),$(SDK_DIR)/platform-tools/adb)
endif

.DEFAULT_GOAL := help

.PHONY: help android install run test lint format check clean devices sdk-check

help: ## Show this list
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  make %-9s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

sdk-check:
	@if [ -z "$(SDK_DIR)" ]; then \
	echo "Android SDK not found. Set ANDROID_HOME, or put sdk.dir=/path/to/sdk in local.properties (Android Studio writes it)." >&2; \
	exit 1; \
	fi

android: sdk-check ## Build the debug APK
	$(GRADLEW) $(GRADLE_FLAGS) assembleDebug
	@echo "APK: $(APK)"

install: sdk-check ## Build the debug APK and install it on the connected device
	$(GRADLEW) $(GRADLE_FLAGS) installDebug

run: install ## Install the debug APK and open the app
	$(ADB) shell monkey -p $(APP_ID) -c android.intent.category.LAUNCHER 1 > /dev/null

test: sdk-check ## Run the unit tests of every module
	$(GRADLEW) $(GRADLE_FLAGS) test

lint: sdk-check ## Check the code style (ktlint)
	$(GRADLEW) $(GRADLE_FLAGS) ktlintCheck

format: sdk-check ## Fix the code style (ktlint)
	$(GRADLEW) $(GRADLE_FLAGS) ktlintFormat

check: sdk-check ## Style, build and unit tests, as the pull request checks run them
	$(GRADLEW) $(GRADLE_FLAGS) ktlintCheck assembleDebug test --continue

clean: sdk-check ## Delete the build output
	$(GRADLEW) $(GRADLE_FLAGS) clean

devices: ## List the connected devices and emulators
	$(ADB) devices
