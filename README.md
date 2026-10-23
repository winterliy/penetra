# Penetra 1.2.5

Minecraft 26.3 Fabric client mod with Kotlin development enabled alongside the existing Java sources. New client initialization and hotkey routing use Kotlin; mixins remain Java because Fabric's Kotlin guidance does not support Kotlin mixin classes. `$find <namespace:block>` searches already-loaded client chunks inside the configured render-distance square; for example, `$find minecraft:diamond_ore` (the `minecraft:` prefix may be omitted). It does not trigger chunk loads. Matches are capped at 1,024.

The loaded-chunk scanning approach was informed by Baritone 26.3's `WorldScanner`; no Baritone source file is copied into this project. Baritone: https://github.com/cabaletta/baritone (LGPL-3.0-or-later).

Build with Java 25 or later using `./gradlew :fabric:remapJar`. The mod JAR is written to `fabric/build/libs/penetra-fabric-1.2.5.jar`. Fabric Language Kotlin 1.14.2 or newer is required at runtime.

To launch the development client on this machine's Java 27 runtime, use `./gradlew :fabric:runClient -Prun_java_version=27`. A regular installed Minecraft 26.3 client must run Java 25 or newer.

Search runs in small client-tick batches (six chunks per tick), checks section palettes before inspecting individual block states, and never requests unloaded chunks. Matches are limited to the first 1,024 results. Found blocks receive depth-independent (x-ray) outlines, so walls do not hide them. Set the outline color with `$color FF8800` (six-digit RGB; an optional leading `#` is accepted), toggle display with `$showon` / `$showoff`, repeat the last search with `$reload`, and clear markers with `$find clear`. Block additions, removals, and replacements update tracked outlines immediately. Typing a Penetra command displays a command-style tooltip list with block ID suggestions.

The mod compiled and packaged successfully, and the development client launched into a local singleplayer world on Java 27. The chat commands and visible block outlines still need an interactive in-game check.

Additional default hotkeys: `Y` KillAura, `U` MultiAura, `[` X-Ray, `]` Freecam, `G` AutoFish, `H` AutoFarm, `J` CameraDistance, `;` AutoAim, and `N` BowAimbot. `$help` displays all Penetra keybinds and commands. Hotkeys use GLFW key codes rather than platform-dependent keyboard scancodes. BowAimbot points at the nearest visible living entity while a bow or crossbow is held; it does not predict projectile travel. These client-side helpers still rely on server-approved interaction packets. AutoFarm currently harvests mature vanilla crops in nearby loaded blocks; it does not automatically replant them.
