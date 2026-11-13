# Homes

Dialog-based homes plugin for Paper **1.21.9+** (also newer 26.x servers). No resource pack needed:
icons are drawn with Minecraft's built-in sprite text component.

## Build
`mvn package` (Java 21) -> `target/Homes-1.0.0.jar`. Pushing to GitHub also builds it via Actions (see the Actions tab -> artifact).
For a 26.x-only server you can change `paper.version` in `pom.xml`.

## Usage
- `/homes` opens the HOMES menu. Click **New Home** to save your location.
- Click a home: Teleport, Change Icon, Rename, Delete.
- Change Icon: type in the box and press Search, Default restores the bed, Back (red) returns.

## Config
See `config.yml`: slots (`max-homes`, or permission `homes.limit.<n>`), button sizes, `keep-gui-open-after-set`,
and `icon-overrides` to fix any icon that renders wrong. `/homes reload` (`homes.admin`) reloads it.
