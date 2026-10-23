# Changelog

## [2.2.1] - 2026-10-24

### Changed
- Adjust third-person camera distance with the mouse wheel while CameraDistance is enabled, and reset it when toggled.
- Keep `$find` outlines visible through walls.

### Removed
- Remove the bracket-key X-Ray toggle and its block and fluid rendering hooks.

## [2.2.0] - 2026-10-24

### Added
- Use the Freecam camera position and view direction for block and entity targeting.
- Filter non-target blocks and fluids in Sodium's chunk renderer while X-Ray is enabled.

### Changed
- Let KillAura and MultiAura send attacks every client tick against hostile targets within six blocks.

## [2.1.2] - 2026-10-24

### Changed
- Name the root compile library `penetra-common` so it cannot be confused with the installable Fabric mod JAR.

## [2.1.1] - 2026-10-24

### Fixed
- Keep KillAura, MultiAura, AutoAim, and BowAimbot from selecting players or non-hostile entities.

## [2.1.0] - 2026-10-24

### Added
- Show enabled and disabled keyboard shortcuts in a small in-game overlay.
- Add Wurst-inspired X-Ray block filtering and default target selection.
- Add an independent Freecam with mouse look, configurable movement keys, scroll speed control, and disable-on-damage behavior.

### Changed
- Refresh rendered sections when X-Ray is toggled.
- Keep Minecraft's normal depth behavior for blocks outside the X-Ray target list.
