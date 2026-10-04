# Item Sway (Forge 1.20.1)

Client-side mod: held items lag, swing and wobble as you turn the camera (GoldenEye / Perfect Dark style).

## Build
1. Download the Forge 1.20.1 MDK (47.3.0) from https://files.minecraftforge.net/ and unzip it.
2. Copy `gradlew`, `gradlew.bat` and the `gradle/` folder from the MDK into this project.
3. Run `./gradlew build` (Java 17 required). The jar lands in `build/libs/`.
4. For development: `./gradlew genIntellijRuns` (or `genEclipseRuns`), then run the client.

## Config
After first launch: `config/itemsway-client.toml`
Main knobs: gain, stiffness, damping, maxAngle, rollAmount, shiftAmount, invertYaw, invertPitch.
If the sway goes the wrong direction, toggle invertYaw / invertPitch.

## Build without installing anything (GitHub Actions)
1. Create a free GitHub account and a new empty repository.
2. Upload every file in this folder (including the hidden `.github` folder) to it.
3. Open the repo's **Actions** tab, wait for "Build mod" to finish (a few minutes).
4. Open the finished run and download the **itemsway-jar** artifact. Unzip it and put the .jar in your mods folder.
Use the jar WITHOUT "-sources" in its name.

## In-game settings
Press **K** in a world (rebindable under Controls -> Item Sway), or use Mods -> Item Sway -> Config.
Every setting has a slider, a per-slider **R** reset button, plus Reset tab / Reset ALL and presets.
Live preview swings a virtual camera while the screen is open so you can watch the item react.
Settings are saved to `config/itemsway-client.toml`. Delete that file to start fresh.
