package com.example.itemsway;

import static com.example.itemsway.SwayParams.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spring-driven item sway. The held item is simulated as a body that chases the
 * camera's orientation (yaw/pitch/roll springs). Runs once per rendered frame
 * with real elapsed time, rotates the item around its own position, and layers
 * movement, bobbing, idle and kick effects on top. Every number comes from
 * {@link SwayParams} so it can be tuned in-game.
 */
@Mod.EventBusSubscriber(modid = ItemSwayMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwayHandler {

    private static final double MIN_DT = 0.0004;      // below this = second hand, same frame
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double REF_SPEED = 180.0;    // deg/s reference for the response curve
    private static final double WALK_SPEED = 4.317;   // blocks/s

    // Vanilla item position in hand-render space (rotation pivot).
    private static final double BASE_PX = 0.56;
    private static final double BASE_PY = -0.52;
    private static final double BASE_PZ = -0.72;

    private static LocalPlayer lastPlayer;
    private static boolean init;
    private static long lastNanos;
    private static float lastViewYaw, lastViewPitch;

    // camera (after gain/curve), smoothed camera, item angles & velocities (deg)
    private static double camYaw, camPitch, smYaw, smPitch;
    private static double itemYaw, itemPitch, yawVel, pitchVel;
    private static double roll, rollVel;

    // results for the current frame
    private static double outYaw, outPitch, outRoll, outPosX, outPosY, outPosZ;

    // movement / events state
    private static double mF, mR, mV, hSpeed, sprint, groundBlend;
    private static double lastVyTick, lastVyBlocks;
    private static float lastFall;
    private static int lastHurt;
    private static boolean lastSwing;
    private static double hurtSign = 1;
    private static double idleT, previewT, previewEventT, fakePhase;
    private static int previewEvent;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ENABLED.on()) {
            init = false;
            return;
        }

        float pt = event.getPartialTick();
        PoseStack ps = event.getPoseStack();

        // Undo vanilla's own sway (a small rotation about the camera).
        if (CANCEL_VANILLA.on()) {
            float vx = (player.getViewXRot(pt) - Mth.lerp(pt, player.xBobO, player.xBob)) * 0.1f;
            float vy = (player.getViewYRot(pt) - Mth.lerp(pt, player.yBobO, player.yBob)) * 0.1f;
            ps.mulPose(Axis.YP.rotationDegrees(-vy));
            ps.mulPose(Axis.XP.rotationDegrees(-vx));
        }

        update(player, pt);

        boolean main = event.getHand() == InteractionHand.MAIN_HAND;
        double hm = main ? 1.0 : OFFHAND.v();
        double master = MASTER.v();
        HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        double side = arm == HumanoidArm.RIGHT ? 1.0 : -1.0;

        float yaw = (float) (outYaw * master * hm + REST_YAW.v());
        float pitch = (float) (outPitch * master * hm + REST_PITCH.v());
        float rollDeg = (float) (outRoll * master * hm + REST_ROLL.v());

        double tx = side * (REST_X.v() + SPRINT_X.v() * sprint) + outPosX * hm;
        double ty = REST_Y.v() + SPRINT_Y.v() * sprint + outPosY * hm;
        double tz = REST_Z.v() + SPRINT_Z.v() * sprint + outPosZ * hm;
        ps.translate(tx, ty, tz);

        double px = side * (BASE_PX + PIVOT_X.v());
        double py = BASE_PY + PIVOT_Y.v();
        double pz = BASE_PZ + PIVOT_Z.v();

        // Rotate about the item's own position so it tilts instead of sliding.
        ps.translate(px, py, pz);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(rollDeg));
        ps.translate(-px, -py, -pz);
    }

    private static void update(LocalPlayer player, float pt) {
        Minecraft mc = Minecraft.getInstance();
        float vYaw = player.getViewYRot(pt);
        float vPitch = player.getViewXRot(pt);
        long now = System.nanoTime();

        if (!init || player != lastPlayer) {
            lastPlayer = player;
            init = true;
            lastNanos = now;
            lastViewYaw = vYaw;
            lastViewPitch = vPitch;
            camYaw = camPitch = smYaw = smPitch = 0;
            itemYaw = itemPitch = yawVel = pitchVel = 0;
            roll = rollVel = 0;
            outYaw = outPitch = outRoll = outPosX = outPosY = outPosZ = 0;
            mF = mR = mV = hSpeed = 0;
            sprint = 0;
            groundBlend = 1;
            lastVyTick = lastVyBlocks = 0;
            lastFall = 0;
            lastHurt = 0;
            lastSwing = false;
            previewT = previewEventT = fakePhase = idleT = 0;
            previewEvent = 0;
            return;
        }

        double dtReal = (now - lastNanos) * 1e-9;
        if (dtReal < MIN_DT) return; // second hand in the same frame: reuse results
        lastNanos = now;
        double dt = Math.min(dtReal, MAX_DT.v()) * TIME_SCALE.v();

        boolean preview = mc.screen instanceof SwayConfigScreen && PREVIEW_ON.on();
        boolean fakeMove = preview && PREVIEW_MOVE.on();
        double per = PREVIEW_PERIOD.v();

        // ------------------------------------------------ camera input
        double rawYaw = Mth.wrapDegrees(vYaw - lastViewYaw);
        double rawPitch = vPitch - lastViewPitch;
        lastViewYaw = vYaw;
        lastViewPitch = vPitch;

        if (preview) {
            double t0 = previewT;
            previewT += dtReal;
            rawYaw += previewYaw(previewT, per) - previewYaw(t0, per);
            rawPitch += previewPitch(previewT, per) - previewPitch(t0, per);
        }

        double dYaw = shape(rawYaw, dtReal) * YAW_INPUT.v();
        double dPitch = shape(rawPitch, dtReal) * PITCH_INPUT.v();

        // ------------------------------------------------ movement sampling
        double mass = Math.max(0.05, WEIGHT.v());
        double fwd, right, hs, vyBlocks, sprintTarget, groundTarget, phase;

        if (fakeMove) {
            double t = previewT;
            fwd = WALK_SPEED * (0.6 + 0.4 * Math.sin(TWO_PI * t / (per * 2.0)));
            right = 2.5 * Math.sin(TWO_PI * t / per);
            hs = Math.hypot(fwd, right);
            vyBlocks = 0;
            sprintTarget = Math.sin(TWO_PI * t / (per * 4.0)) > 0.4 ? 1 : 0;
            groundTarget = 1;
            fakePhase += Math.PI * 0.6 * hs * dtReal;
            phase = fakePhase;

            previewEventT += dtReal;
            if (previewEventT > per * 1.5) {
                previewEventT = 0;
                applyKick(previewEvent, mass, 6.0);
                previewEvent = (previewEvent + 1) % 4;
            }
        } else {
            Vec3 dm = player.getDeltaMovement();
            double vx = dm.x * 20.0, vz = dm.z * 20.0;
            vyBlocks = dm.y * 20.0;
            double yawRad = Math.toRadians(vYaw);
            fwd = -vx * Math.sin(yawRad) + vz * Math.cos(yawRad);
            right = -(vx * Math.cos(yawRad) + vz * Math.sin(yawRad));
            hs = Math.hypot(vx, vz);
            sprintTarget = player.isSprinting() ? 1 : 0;
            groundTarget = player.fallDistance < 0.1f ? 1 : 0;
            phase = Math.PI * Mth.lerp(pt, player.walkDistO, player.walkDist);

            // jump
            if (dm.y > 0.3 && lastVyTick <= 0.3) applyKick(0, mass, 0);
            // landing
            if (lastFall > 0.5f && player.fallDistance == 0f) applyKick(1, mass, Math.max(0, -lastVyBlocks));
            // damage
            int h = player.hurtTime;
            if (h > lastHurt) applyKick(2, mass, 0);
            lastHurt = h;
            // swing
            boolean sw = player.swinging;
            if (sw && !lastSwing) applyKick(3, mass, 0);
            lastSwing = sw;

            lastVyTick = dm.y;
            lastVyBlocks = vyBlocks;
            lastFall = player.fallDistance;
        }

        double tauM = MOVE_SMOOTH.v();
        double aM = tauM <= 1e-4 ? 1.0 : 1.0 - Math.exp(-dtReal / tauM);
        mF += (fwd - mF) * aM;
        mR += (right - mR) * aM;
        mV += (vyBlocks - mV) * aM;
        hSpeed += (hs - hSpeed) * aM;
        sprint += (sprintTarget - sprint) * (1.0 - Math.exp(-dtReal / Math.max(0.01, SPRINT_BLEND.v())));
        groundBlend += (groundTarget - groundBlend) * (1.0 - Math.exp(-dtReal / 0.1));

        // ------------------------------------------------ walk bob + idle + equilibrium shifts
        double mv = Mth.clamp(hSpeed / WALK_SPEED, 0.0, 1.5);
        double walkAmp = mv * groundBlend * (1.0 + (WALK_SPRINT.v() - 1.0) * sprint);
        double sinP = Math.sin(phase);
        double sin2P = Math.sin(2.0 * phase);

        idleT += dtReal;
        double iw = TWO_PI * IDLE_FREQ.v();
        double fade = 1.0 - IDLE_FADE.v() * Math.min(1.0, mv);
        double idleY = IDLE_YAW.v() * Math.sin(iw * idleT) * fade;
        double idleP = IDLE_PITCH.v() * Math.sin(iw * idleT * 0.83 + 1.3) * fade;
        double idleR = IDLE_ROLL.v() * Math.sin(iw * idleT * 0.61 + 2.1) * fade;

        double shiftYaw = STRAFE_YAW.v() * mR + SPRINT_YAW.v() * sprint + WALK_YAW.v() * sinP * walkAmp + idleY;
        double shiftPitch = FWD_PITCH.v() * mF + VERT_PITCH.v() * mV + SPRINT_PITCH.v() * sprint
                + WALK_PITCH.v() * sin2P * walkAmp + idleP - WEIGHT_DROOP.v() * mass;
        double shiftRoll = STRAFE_ROLL.v() * mR + SPRINT_ROLL.v() * sprint + WALK_ROLL.v() * sinP * walkAmp + idleR;
        double walkPosX = WALK_X.v() * sinP * walkAmp;
        double walkPosY = WALK_Y.v() * sin2P * walkAmp;

        // ------------------------------------------------ spring simulation
        double massF = 1.0 / Math.sqrt(mass);
        double wy = TWO_PI * YAW_FREQ.v() * massF;
        double wp = TWO_PI * PITCH_FREQ.v() * massF;
        boolean rollInstant = ROLL_FREQ.v() >= 39.99;
        double wr = TWO_PI * ROLL_FREQ.v() * massF;
        double wMax = Math.max(wy, Math.max(wp, rollInstant ? 0.0 : wr));
        double hMax = Math.min(1.0 / SUB_RATE.v(), 0.4 / Math.max(wMax, 1e-3));
        int steps = (int) Mth.clamp(Math.ceil(dt / hMax), 1, 4000);
        double h = dt / steps;

        double zy = YAW_DAMP.v(), zp = PITCH_DAMP.v(), zr = ROLL_DAMP.v();
        double qy = YAW_QDRAG.v(), qp = PITCH_QDRAG.v();
        double sY = YAW_STRENGTH.v(), sP = PITCH_STRENGTH.v();
        double mxY = YAW_MAX.v(), mxP = PITCH_MAX.v();
        double sfY = YAW_SOFT.v(), sfP = PITCH_SOFT.v();
        double rfY = ROLL_FROM_YAW.v(), rfP = ROLL_FROM_PITCH.v(), rfS = ROLL_FROM_SPEED.v();
        double clampMult = INTERNAL_CLAMP.v();
        double velLimit = VEL_LIMIT.v();
        boolean limitVel = velLimit < 4999.5;
        double tau = IN_SMOOTH.v();
        double aS = tau <= 1e-5 ? 1.0 : 1.0 - Math.exp(-h / tau);

        double prevCamYaw = camYaw, prevCamPitch = camPitch;
        camYaw += dYaw;
        camPitch += dPitch;

        double offY = camYaw - itemYaw;
        double offP = camPitch - itemPitch;

        for (int i = 1; i <= steps; i++) {
            double f = (double) i / steps;
            double camY = prevCamYaw + (camYaw - prevCamYaw) * f;
            double camP = prevCamPitch + (camPitch - prevCamPitch) * f;

            smYaw += (camY - smYaw) * aS;
            smPitch += (camP - smPitch) * aS;

            double tY = smYaw - shiftYaw;
            double tP = smPitch - shiftPitch;

            yawVel = (yawVel + wy * wy * (tY - itemYaw) * h) / (1.0 + (2.0 * zy * wy + qy * Math.abs(yawVel)) * h);
            pitchVel = (pitchVel + wp * wp * (tP - itemPitch) * h) / (1.0 + (2.0 * zp * wp + qp * Math.abs(pitchVel)) * h);
            if (limitVel) {
                yawVel = Mth.clamp(yawVel, -velLimit, velLimit);
                pitchVel = Mth.clamp(pitchVel, -velLimit, velLimit);
            }
            itemYaw += yawVel * h;
            itemPitch += pitchVel * h;

            // keep the internal lag from running away after huge spins
            double limY = Math.max(mxY, 1.0) * clampMult;
            double limP = Math.max(mxP, 1.0) * clampMult;
            offY = camY - itemYaw;
            offP = camP - itemPitch;
            if (offY > limY) { itemYaw = camY - limY; offY = limY; }
            else if (offY < -limY) { itemYaw = camY + limY; offY = -limY; }
            if (offP > limP) { itemPitch = camP - limP; offP = limP; }
            else if (offP < -limP) { itemPitch = camP + limP; offP = -limP; }

            // roll
            double yD = softLimit(offY * sY, mxY, sfY);
            double pD = softLimit(offP * sP, mxP, sfP);
            double rTarget = rfY * yD + rfP * pD + rfS * (-yawVel) + shiftRoll;
            if (rollInstant) {
                roll = rTarget;
                rollVel = 0;
            } else {
                rollVel = (rollVel + wr * wr * (rTarget - roll) * h) / (1.0 + 2.0 * zr * wr * h);
                roll += rollVel * h;
            }
        }

        // ------------------------------------------------ outputs
        double yD = softLimit(offY * sY, mxY, sfY);
        double pD = softLimit(offP * sP, mxP, sfP);
        outYaw = yD + PITCH_TO_YAW.v() * pD;
        outPitch = pD + YAW_TO_PITCH.v() * yD;
        outRoll = softLimit(roll * ROLL_STRENGTH.v(), ROLL_MAX.v(), ROLL_SOFT.v());
        outPosX = POS_X_YAW.v() * yD + walkPosX;
        outPosY = POS_Y_PITCH.v() * pD + walkPosY;
        outPosZ = POS_Z_SPEED.v() * (Math.abs(yawVel) + Math.abs(pitchVel)) / 100.0;
    }

    /** Sudden velocity kicks. Positive amount = flick up / left. */
    private static void applyKick(int type, double mass, double fallSpeed) {
        switch (type) {
            case 0 -> pitchVel -= JUMP_KICK.v() / mass;
            case 1 -> pitchVel -= LAND_KICK.v() * fallSpeed / mass;
            case 2 -> {
                hurtSign = -hurtSign;
                pitchVel -= HURT_KICK_PITCH.v() / mass;
                yawVel -= HURT_KICK_YAW.v() * hurtSign / mass;
            }
            default -> pitchVel -= ATTACK_KICK.v() / mass;
        }
    }

    /** Deadzone + response curve on camera motion. */
    private static double shape(double delta, double dt) {
        double s = delta / dt;
        double a = Math.max(0.0, Math.abs(s) - DEADZONE.v());
        double p = CURVE.v();
        if (p != 1.0 && a > 0.0) a = REF_SPEED * Math.pow(a / REF_SPEED, p);
        return Math.copySign(a, s) * dt;
    }

    private static double previewYaw(double t, double period) {
        return PREVIEW_YAW.v() * Math.tanh(3.0 * Math.sin(TWO_PI * t / period));
    }

    private static double previewPitch(double t, double period) {
        return PREVIEW_PITCH.v() * Math.tanh(3.0 * Math.sin(TWO_PI * t / (period * 1.37) + 1.0));
    }

    /** Blend between a hard clamp (soft=0) and a smooth tanh curve (soft=1). */
    private static double softLimit(double x, double max, double soft) {
        if (max <= 0.0) return 0.0;
        double hard = Mth.clamp(x, -max, max);
        double smooth = max * Math.tanh(x / max);
        return hard + (smooth - hard) * soft;
    }
}
