# Penetra 1.2.3

Minecraft 26.3 Fabric client mod. `$find <namespace:block>` searches already-loaded client chunks inside the configured render-distance square; for example, `$find minecraft:diamond_ore` (the `minecraft:` prefix may be omitted). It does not trigger chunk loads. Matches are capped at 1,024.

The loaded-chunk scanning approach was informed by Baritone 26.3's `WorldScanner`; no Baritone source file is copied into this project. Baritone: https://github.com/cabaletta/baritone (LGPL-3.0-or-later).

Build with Java 25 or later using `./gradlew :fabric:remapJar`. The mod JAR is written to `fabric/build/libs/penetra-fabric-1.2.3.jar`.

To launch the development client on this machine's Java 27 runtime, use `./gradlew :fabric:runClient -Prun_java_version=27`. A regular installed Minecraft 26.3 client must run Java 25 or newer.

Search runs in small client-tick batches (six chunks per tick), checks section palettes before inspecting individual block states, and never requests unloaded chunks. Matches are limited to the first 1,024 results. Found blocks receive depth-independent (x-ray) outlines, so walls do not hide them. Set the outline color with `$color FF8800` (six-digit RGB; an optional leading `#` is accepted), toggle display with `$showon` / `$showoff`, repeat the last search with `$reload`, and clear markers with `$find clear`. Block additions, removals, and replacements update tracked outlines immediately. Typing a Penetra command displays a command-style tooltip list with block ID suggestions.

The mod compiled and packaged successfully, and the development client launched into a local singleplayer world on Java 27. The chat commands and visible block outlines still need an interactive in-game check.
