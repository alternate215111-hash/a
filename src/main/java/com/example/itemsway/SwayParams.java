package com.example.itemsway;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every setting in the mod. The config file, the in-game screen and the
 * simulation all read from this single list.
 *
 * Sign convention for tilt: positive yaw = item points LEFT, positive pitch =
 * item points UP, positive roll = counter-clockwise twist.
 */
public final class SwayParams {
    private SwayParams() {}

    public record Cat(String id, String tab, String title) {}
    public record Preset(String name, Map<String, Double> values) {}

    public static final List<Cat> CATS = new ArrayList<>();
    public static final List<Param> ALL = new ArrayList<>();
    public static final List<Preset> PRESETS = new ArrayList<>();

    private static Cat cat(String id, String tab, String title) {
        Cat c = new Cat(id, tab, title);
        CATS.add(c);
        return c;
    }

    private static Param num(Cat c, String id, String label, String unit, double def,
                             double min, double max, double step, String desc) {
        Param p = new Param(c, id, label, unit, def, min, max, step, false, desc);
        ALL.add(p);
        return p;
    }

    private static Param flag(Cat c, String id, String label, boolean def, String desc) {
        Param p = new Param(c, id, label, "", def ? 1 : 0, 0, 1, 1, true, desc);
        ALL.add(p);
        return p;
    }

    // ------------------------------------------------------------------ categories
    public static final Cat GENERAL = cat("general", "Gen", "General");
    public static final Cat YAW = cat("yaw", "Yaw", "Yaw (left/right tilt)");
    public static final Cat PITCH = cat("pitch", "Pitch", "Pitch (up/down tilt)");
    public static final Cat ROLL = cat("roll", "Roll", "Roll (twist)");
    public static final Cat MIX = cat("mixing", "Mix", "Mixing & limits");
    public static final Cat INPUT = cat("input", "Input", "Camera input");
    public static final Cat PHYS = cat("physics", "Phys", "Advanced physics");
    public static final Cat REST = cat("rest", "Rest", "Rest pose & pivot");
    public static final Cat POS = cat("position", "Pos", "Position sway");
    public static final Cat MOVE = cat("movement", "Move", "Movement reaction");
    public static final Cat BOB = cat("bobbing", "Bob", "Walk bob & idle");
    public static final Cat PREVIEW = cat("preview", "Prev", "Live preview");

    // ------------------------------------------------------------------ general
    public static final Param ENABLED = flag(GENERAL, "enabled", "Mod enabled", true,
            "Master on/off switch for the whole effect.");
    public static final Param CANCEL_VANILLA = flag(GENERAL, "cancelVanillaSway", "Cancel vanilla sway", true,
            "Removes vanilla's own small hand sway so it doesn't stack with this mod.");
    public static final Param MASTER = num(GENERAL, "masterStrength", "Master strength", "x", 1.0, 0, 5, 0.05,
            "Final multiplier on all tilt output. 1 = normal, 0 = no tilt.");
    public static final Param WEIGHT = num(GENERAL, "weight", "Weight (mass)", "x", 1.0, 0.1, 10, 0.05,
            "How heavy the item feels. Heavier = slower to react and recover, and kicks (jump, landing, hit) hit softer.");
    public static final Param WEIGHT_DROOP = num(GENERAL, "weightDroop", "Weight droop", "deg", 0, -30, 30, 0.5,
            "Resting downward tilt per unit of weight. Heavier items sag more. Negative = tilts up instead.");
    public static final Param TIME_SCALE = num(GENERAL, "timeScale", "Physics speed", "x", 1.0, 0.1, 3, 0.05,
            "Speed of the simulation. Below 1 = slow motion, above 1 = fast forward.");
    public static final Param OFFHAND = num(GENERAL, "offhandEffect", "Off-hand effect", "x", 1.0, 0, 2, 0.05,
            "Strength of the sway on the off-hand item relative to the main hand.");

    // ------------------------------------------------------------------ yaw
    public static final Param YAW_INPUT = num(YAW, "yawInputGain", "Camera input gain", "x", 1.0, -3, 3, 0.05,
            "How much of your left/right camera turning the item feels. Negative = item leads the turn.");
    public static final Param YAW_FREQ = num(YAW, "yawFrequency", "Spring frequency", "Hz", 2.2, 0.2, 20, 0.05,
            "How fast the item catches up with the camera. Lower = slower, floatier, longer lag.");
    public static final Param YAW_DAMP = num(YAW, "yawDamping", "Damping ratio", "", 0.45, 0.02, 3, 0.01,
            "How quickly wobble dies out. ~1 = no overshoot, 0.4-0.6 = slight bounce, <0.25 = very bouncy.");
    public static final Param YAW_QDRAG = num(YAW, "yawSpeedDrag", "Speed drag", "", 0, 0, 0.05, 0.001,
            "Extra braking that grows with speed. Tames violent whips without deadening slow motion.");
    public static final Param YAW_STRENGTH = num(YAW, "yawStrength", "Output strength", "x", 1.0, -3, 3, 0.05,
            "Multiplier on the left/right tilt. Negative flips the direction.");
    public static final Param YAW_MAX = num(YAW, "yawMaxAngle", "Max angle", "deg", 14, 0, 90, 0.5,
            "Largest left/right tilt the item can reach.");
    public static final Param YAW_SOFT = num(YAW, "yawLimitSoftness", "Limit softness", "", 1.0, 0, 1, 0.05,
            "0 = hard wall at the max angle, 1 = tilt eases gradually toward it.");

    // ------------------------------------------------------------------ pitch
    public static final Param PITCH_INPUT = num(PITCH, "pitchInputGain", "Camera input gain", "x", 1.0, -3, 3, 0.05,
            "How much of your up/down camera turning the item feels. Negative = item leads the turn.");
    public static final Param PITCH_FREQ = num(PITCH, "pitchFrequency", "Spring frequency", "Hz", 2.2, 0.2, 20, 0.05,
            "How fast the item catches up with the camera. Lower = slower, floatier, longer lag.");
    public static final Param PITCH_DAMP = num(PITCH, "pitchDamping", "Damping ratio", "", 0.45, 0.02, 3, 0.01,
            "How quickly wobble dies out. ~1 = no overshoot, 0.4-0.6 = slight bounce, <0.25 = very bouncy.");
    public static final Param PITCH_QDRAG = num(PITCH, "pitchSpeedDrag", "Speed drag", "", 0, 0, 0.05, 0.001,
            "Extra braking that grows with speed. Tames violent whips without deadening slow motion.");
    public static final Param PITCH_STRENGTH = num(PITCH, "pitchStrength", "Output strength", "x", 1.0, -3, 3, 0.05,
            "Multiplier on the up/down tilt. Negative flips the direction.");
    public static final Param PITCH_MAX = num(PITCH, "pitchMaxAngle", "Max angle", "deg", 14, 0, 90, 0.5,
            "Largest up/down tilt the item can reach.");
    public static final Param PITCH_SOFT = num(PITCH, "pitchLimitSoftness", "Limit softness", "", 1.0, 0, 1, 0.05,
            "0 = hard wall at the max angle, 1 = tilt eases gradually toward it.");

    // ------------------------------------------------------------------ roll
    public static final Param ROLL_FROM_YAW = num(ROLL, "rollFromYaw", "Roll from left/right sway", "x", 0.25, -3, 3, 0.01,
            "Twist added in proportion to the left/right tilt.");
    public static final Param ROLL_FROM_PITCH = num(ROLL, "rollFromPitch", "Roll from up/down sway", "x", 0, -3, 3, 0.01,
            "Twist added in proportion to the up/down tilt.");
    public static final Param ROLL_FROM_SPEED = num(ROLL, "rollFromSwaySpeed", "Roll from sway speed", "", 0, -0.1, 0.1, 0.001,
            "Twist added by how fast the item is swinging (degrees of twist per degree/second).");
    public static final Param ROLL_FREQ = num(ROLL, "rollFrequency", "Roll spring frequency", "Hz", 40, 0.5, 40, 0.5,
            "How fast the twist follows its target. 40 = instant (no smoothing). Lower = lazier twist.");
    public static final Param ROLL_DAMP = num(ROLL, "rollDamping", "Roll damping ratio", "", 0.7, 0.02, 3, 0.01,
            "Wobble of the twist. Only matters when roll frequency is below 40.");
    public static final Param ROLL_STRENGTH = num(ROLL, "rollStrength", "Roll output strength", "x", 1.0, -3, 3, 0.05,
            "Multiplier on the final twist. Negative flips the direction.");
    public static final Param ROLL_MAX = num(ROLL, "rollMaxAngle", "Roll max angle", "deg", 90, 0, 180, 1,
            "Largest twist the item can reach.");
    public static final Param ROLL_SOFT = num(ROLL, "rollLimitSoftness", "Roll limit softness", "", 1.0, 0, 1, 0.05,
            "0 = hard wall at the max angle, 1 = twist eases gradually toward it.");

    // ------------------------------------------------------------------ mixing & limits
    public static final Param YAW_TO_PITCH = num(MIX, "yawToPitch", "Yaw adds to pitch", "x", 0, -2, 2, 0.01,
            "Left/right tilt also tips the item up or down, making diagonal arcs.");
    public static final Param PITCH_TO_YAW = num(MIX, "pitchToYaw", "Pitch adds to yaw", "x", 0, -2, 2, 0.01,
            "Up/down tilt also turns the item sideways, making diagonal arcs.");
    public static final Param VEL_LIMIT = num(MIX, "velocityLimit", "Max swing speed", "deg/s", 5000, 50, 5000, 50,
            "Caps how fast the item can swing. 5000 = no cap. Low values give a slow, heavy swing.");
    public static final Param INTERNAL_CLAMP = num(MIX, "runawayLimit", "Runaway limit", "x", 10, 1, 50, 0.5,
            "How far (as a multiple of the max angle) the item can fall behind internally. Lower = recovers sooner after huge spins.");

    // ------------------------------------------------------------------ input
    public static final Param IN_SMOOTH = num(INPUT, "inputSmoothing", "Input smoothing", "s", 0, 0, 0.5, 0.005,
            "Smooths out camera motion before the item sees it. Removes jitter, adds a soft delay.");
    public static final Param DEADZONE = num(INPUT, "deadzone", "Deadzone", "deg/s", 0, 0, 100, 1,
            "Ignores camera turns slower than this. Stops tiny mouse movements from moving the item.");
    public static final Param CURVE = num(INPUT, "responseCurve", "Response curve", "", 1.0, 0.3, 3, 0.05,
            "1 = linear. Above 1: slow turns barely move the item, fast flicks move it a lot. Below 1: the opposite.");

    // ------------------------------------------------------------------ physics
    public static final Param SUB_RATE = num(PHYS, "simulationRate", "Simulation rate", "Hz", 240, 60, 1000, 10,
            "Internal physics steps per second. Higher = more accurate at high frequencies, slightly more CPU.");
    public static final Param MAX_DT = num(PHYS, "maxFrameTime", "Max frame time", "s", 0.05, 0.01, 0.2, 0.005,
            "Longest frame the physics will simulate in one go. Prevents explosions after lag spikes.");

    // ------------------------------------------------------------------ rest pose
    public static final Param REST_YAW = num(REST, "restYaw", "Rest yaw", "deg", 0, -45, 45, 0.5,
            "Permanent left/right tilt of the item. Positive = points left.");
    public static final Param REST_PITCH = num(REST, "restPitch", "Rest pitch", "deg", 0, -45, 45, 0.5,
            "Permanent up/down tilt of the item. Positive = points up.");
    public static final Param REST_ROLL = num(REST, "restRoll", "Rest roll", "deg", 0, -45, 45, 0.5,
            "Permanent twist of the item.");
    public static final Param REST_X = num(REST, "restX", "Rest offset X", "", 0, -0.5, 0.5, 0.005,
            "Moves the item sideways. Positive = away from the center of the screen.");
    public static final Param REST_Y = num(REST, "restY", "Rest offset Y", "", 0, -0.5, 0.5, 0.005,
            "Moves the item up (positive) or down (negative).");
    public static final Param REST_Z = num(REST, "restZ", "Rest offset Z", "", 0, -0.5, 0.5, 0.005,
            "Moves the item toward you (positive) or away (negative).");
    public static final Param PIVOT_X = num(REST, "pivotX", "Pivot offset X", "", 0, -1, 1, 0.01,
            "Shifts the point the item rotates around, sideways. Moving it changes how the swing arcs.");
    public static final Param PIVOT_Y = num(REST, "pivotY", "Pivot offset Y", "", 0, -1, 1, 0.01,
            "Shifts the rotation point up/down. Raise it toward the hand/wrist for a more pendulum-like swing.");
    public static final Param PIVOT_Z = num(REST, "pivotZ", "Pivot offset Z", "", 0, -1, 1, 0.01,
            "Shifts the rotation point toward or away from you. Positive moves it toward you (bigger arc).");

    // ------------------------------------------------------------------ position sway
    public static final Param POS_X_YAW = num(POS, "posXFromYaw", "Slide sideways with yaw", "/deg", 0, -0.02, 0.02, 0.0005,
            "Slides the item left/right in proportion to its left/right tilt. 0 = tilt only, no sliding.");
    public static final Param POS_Y_PITCH = num(POS, "posYFromPitch", "Slide up/down with pitch", "/deg", 0, -0.02, 0.02, 0.0005,
            "Slides the item up/down in proportion to its up/down tilt.");
    public static final Param POS_Z_SPEED = num(POS, "posZFromSwaySpeed", "Push in/out with swing speed", "", 0, -0.3, 0.3, 0.005,
            "Pushes the item toward/away from you the faster it swings (per 100 deg/s).");

    // ------------------------------------------------------------------ movement
    public static final Param MOVE_SMOOTH = num(MOVE, "movementSmoothing", "Movement smoothing", "s", 0.12, 0, 1, 0.01,
            "Smooths walking/jumping speed before it affects the item. Higher = lazier reactions.");
    public static final Param STRAFE_YAW = num(MOVE, "strafeYaw", "Strafe yaw", "deg", 0, -5, 5, 0.05,
            "Left/right tilt per block/second of sideways walking. Positive = points left when strafing right.");
    public static final Param STRAFE_ROLL = num(MOVE, "strafeRoll", "Strafe roll", "deg", 0, -5, 5, 0.05,
            "Twist per block/second of sideways walking.");
    public static final Param FWD_PITCH = num(MOVE, "forwardPitch", "Forward-speed pitch", "deg", 0, -5, 5, 0.05,
            "Up/down tilt per block/second of forward walking. Positive = tips up while running forward.");
    public static final Param VERT_PITCH = num(MOVE, "verticalPitch", "Vertical-speed pitch", "deg", 0, -5, 5, 0.05,
            "Up/down tilt per block/second of rising/falling. Positive = tips up while rising.");
    public static final Param SPRINT_BLEND = num(MOVE, "sprintBlendTime", "Sprint blend time", "s", 0.15, 0.01, 1, 0.01,
            "How quickly the sprint pose fades in and out.");
    public static final Param SPRINT_YAW = num(MOVE, "sprintYaw", "Sprint yaw", "deg", 0, -45, 45, 0.5,
            "Left/right tilt added while sprinting.");
    public static final Param SPRINT_PITCH = num(MOVE, "sprintPitch", "Sprint pitch", "deg", 0, -45, 45, 0.5,
            "Up/down tilt added while sprinting. Negative = lowers the item.");
    public static final Param SPRINT_ROLL = num(MOVE, "sprintRoll", "Sprint roll", "deg", 0, -45, 45, 0.5,
            "Twist added while sprinting.");
    public static final Param SPRINT_X = num(MOVE, "sprintX", "Sprint offset X", "", 0, -0.3, 0.3, 0.005,
            "Sideways shift while sprinting. Positive = away from screen center.");
    public static final Param SPRINT_Y = num(MOVE, "sprintY", "Sprint offset Y", "", 0, -0.3, 0.3, 0.005,
            "Up/down shift while sprinting.");
    public static final Param SPRINT_Z = num(MOVE, "sprintZ", "Sprint offset Z", "", 0, -0.3, 0.3, 0.005,
            "In/out shift while sprinting. Positive = toward you.");
    public static final Param JUMP_KICK = num(MOVE, "jumpKick", "Jump kick", "deg/s", 0, -600, 600, 5,
            "Sudden swing when you jump. Positive = item flicks up, negative = down. Softer on heavy items.");
    public static final Param LAND_KICK = num(MOVE, "landKick", "Landing kick", "deg/s", 0, -300, 300, 5,
            "Swing on landing, per block/second of fall speed. Positive = flicks up, negative = dips down.");
    public static final Param HURT_KICK_PITCH = num(MOVE, "hurtKickPitch", "Damage kick (up/down)", "deg/s", 0, -1000, 1000, 10,
            "Up/down jolt when you take damage. Positive = flicks up.");
    public static final Param HURT_KICK_YAW = num(MOVE, "hurtKickYaw", "Damage kick (sideways)", "deg/s", 0, -1000, 1000, 10,
            "Sideways jolt when you take damage. Alternates direction each hit.");
    public static final Param ATTACK_KICK = num(MOVE, "attackKick", "Swing kick", "deg/s", 0, -1000, 1000, 10,
            "Jolt when you swing/attack/use an item. Positive = flicks up.");

    // ------------------------------------------------------------------ bob & idle
    public static final Param WALK_YAW = num(BOB, "walkBobYaw", "Walk bob yaw", "deg", 0, -10, 10, 0.1,
            "Left/right rocking while walking (once per two steps).");
    public static final Param WALK_PITCH = num(BOB, "walkBobPitch", "Walk bob pitch", "deg", 0, -10, 10, 0.1,
            "Up/down nodding while walking (once per step).");
    public static final Param WALK_ROLL = num(BOB, "walkBobRoll", "Walk bob roll", "deg", 0, -10, 10, 0.1,
            "Twisting while walking (once per two steps).");
    public static final Param WALK_X = num(BOB, "walkBobX", "Walk bob X", "", 0, -0.1, 0.1, 0.002,
            "Side-to-side sliding while walking.");
    public static final Param WALK_Y = num(BOB, "walkBobY", "Walk bob Y", "", 0, -0.1, 0.1, 0.002,
            "Up/down bouncing while walking.");
    public static final Param WALK_SPRINT = num(BOB, "walkBobSprintBoost", "Sprint bob boost", "x", 1.5, 0, 4, 0.05,
            "Multiplier on all walk bob while sprinting.");
    public static final Param IDLE_YAW = num(BOB, "idleYaw", "Idle sway yaw", "deg", 0, -5, 5, 0.05,
            "Slow left/right drift while standing still, like breathing.");
    public static final Param IDLE_PITCH = num(BOB, "idlePitch", "Idle sway pitch", "deg", 0, -5, 5, 0.05,
            "Slow up/down drift while standing still.");
    public static final Param IDLE_ROLL = num(BOB, "idleRoll", "Idle sway roll", "deg", 0, -5, 5, 0.05,
            "Slow twist drift while standing still.");
    public static final Param IDLE_FREQ = num(BOB, "idleFrequency", "Idle sway speed", "Hz", 0.25, 0.02, 3, 0.01,
            "How fast the idle drift cycles.");
    public static final Param IDLE_FADE = num(BOB, "idleFadeWhenMoving", "Idle fades when moving", "", 0.7, 0, 1, 0.05,
            "How much the idle drift fades out while you walk. 1 = fully, 0 = not at all.");

    // ------------------------------------------------------------------ preview
    public static final Param PREVIEW_ON = flag(PREVIEW, "previewEnabled", "Live preview", true,
            "While this screen is open, the camera is virtually swung left/right/up/down so you can watch the item react.");
    public static final Param PREVIEW_MOVE = flag(PREVIEW, "previewMovement", "Preview movement", false,
            "Also fakes walking, sprinting, strafing, jumping, landing, damage and swinging so you can tune those too.");
    public static final Param PREVIEW_YAW = num(PREVIEW, "previewYawAmount", "Preview left/right swing", "deg", 40, 0, 180, 1,
            "How far the fake camera swings left/right.");
    public static final Param PREVIEW_PITCH = num(PREVIEW, "previewPitchAmount", "Preview up/down swing", "deg", 12, 0, 90, 1,
            "How far the fake camera swings up/down.");
    public static final Param PREVIEW_PERIOD = num(PREVIEW, "previewPeriod", "Preview cycle time", "s", 3.0, 0.5, 12, 0.1,
            "Seconds per full left-right swing. Lower = faster, harsher flicks.");

    // ------------------------------------------------------------------ presets
    private static void preset(String name, Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        }
        PRESETS.add(new Preset(name, m));
    }

    static {
        preset("Default (current)");
        preset("Heavy",
                "weight", 2.0, "yawFrequency", 1.8, "pitchFrequency", 1.8,
                "yawDamping", 0.4, "pitchDamping", 0.4,
                "yawMaxAngle", 22, "pitchMaxAngle", 18, "masterStrength", 1.4);
        preset("Floaty",
                "yawFrequency", 1.1, "pitchFrequency", 1.1,
                "yawDamping", 0.22, "pitchDamping", 0.22,
                "yawMaxAngle", 30, "pitchMaxAngle", 24,
                "masterStrength", 1.6, "rollFromYaw", 0.5);
        preset("Snappy",
                "yawFrequency", 5.5, "pitchFrequency", 5.5,
                "yawDamping", 0.7, "pitchDamping", 0.7,
                "yawMaxAngle", 10, "pitchMaxAngle", 10);
        preset("Bouncy 64",
                "yawInputGain", 1.6, "pitchInputGain", 1.3,
                "yawFrequency", 2.6, "pitchFrequency", 2.6,
                "yawDamping", 0.28, "pitchDamping", 0.28,
                "yawMaxAngle", 28, "pitchMaxAngle", 20,
                "rollFromYaw", 0.45, "rollFrequency", 8);
    }

    public static Param byId(String id) {
        for (Param p : ALL) if (p.id.equals(id)) return p;
        return null;
    }

    public static void resetAll() {
        for (Param p : ALL) p.value = p.def;
    }

    public static void applyPreset(Preset pr) {
        for (Param p : ALL) {
            if (p.cat == PREVIEW || p == ENABLED) continue;
            p.value = p.def;
        }
        for (Map.Entry<String, Double> e : pr.values().entrySet()) {
            Param p = byId(e.getKey());
            if (p != null) p.value = p.clamp(e.getValue());
        }
    }
}
