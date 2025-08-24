package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ClientModInitializer {
    public static final String MOD_ID = "smoothestaimbotever";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final double SPEED = 0.7;
    private static final double STEP_SIZE = 0.05;
    private static final double JITTER = 0.0015; // smaller jitter for smoother aim
    private static final double AIM_SPEED = 0.045; // faster smooth aim per tick
    private static final long SLEEP_MS = 5; // faster update rate

    @Override
    public void onInitializeClient() {
        LOGGER.info("Hyper-optimized smooth micro-lock aimbot enabled.");
        MinecraftClient client = MinecraftClient.getInstance();

        new Thread(() -> {
            while (true) {
                ClientPlayerEntity player = client.player;
                if (player != null) {
                    movePlayer(player);
                    targetPlayer(player, client);
                }
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void movePlayer(ClientPlayerEntity player) {
        Vec3d motion = player.getRotationVector()
                .multiply(SPEED * STEP_SIZE)
                .add((Math.random() - 0.5) * JITTER, 0, (Math.random() - 0.5) * JITTER);
        player.setVelocity(player.getVelocity().add(motion));
        player.velocityModified = true;
    }

    private void targetPlayer(ClientPlayerEntity player, MinecraftClient client) {
        PlayerEntity target = client.world.getPlayers().stream()
                .filter(p -> !p.getUuid().equals(player.getUuid()))
                .min((p1, p2) -> Double.compare(p1.squaredDistanceTo(player), p2.squaredDistanceTo(player)))
                .orElse(null);

        if (target != null) {
            Vec3d dir = new Vec3d(
                    target.getX() - player.getX(),
                    target.getEyeY() - player.getEyeY(),
                    target.getZ() - player.getZ()
            ).normalize();

            Vec3d look = player.getRotationVector();
            Vec3d smoothLook = look.add(dir.subtract(look).multiply(AIM_SPEED));
            ((EntityAccessor) player).callSetRotation(
                    (float) Math.toDegrees(Math.atan2(smoothLook.z, smoothLook.x)) - 90f,
                    (float) Math.toDegrees(Math.asin(smoothLook.y))
            );
        }
    }
}