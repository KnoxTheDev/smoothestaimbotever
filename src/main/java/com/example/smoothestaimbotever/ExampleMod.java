package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ClientModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static final double SPEED = 0.7; // high speed
    private static final double STEP_SIZE = 0.05; // tiny smooth steps
    private static final double JITTER = 0.002; // slight jitter for micro aiming
    private static final double AIM_SPEED = 0.03; // micro aim adjustment per tick

    @Override
    public void onInitializeClient() {
        LOGGER.info("Advanced player movement with micro-lock targeting for players enabled.");
        MinecraftClient client = MinecraftClient.getInstance();

        new Thread(() -> {
            while (true) {
                if (client.player != null) {
                    movePlayer(client.player);
                    targetPlayer(client.player, client);
                }
                try {
                    Thread.sleep(10); // control update rate
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void movePlayer(ClientPlayerEntity player) {
        Vec3d forward = player.getRotationVector().multiply(SPEED * STEP_SIZE);
        double jitterX = (Math.random() - 0.5) * JITTER;
        double jitterZ = (Math.random() - 0.5) * JITTER;
        Vec3d motion = forward.add(jitterX, 0, jitterZ);

        player.setVelocity(player.getVelocity().add(motion));
        player.velocityModified = true;
    }

    private void targetPlayer(ClientPlayerEntity player, MinecraftClient client) {
        PlayerEntity closestPlayer = client.world.getPlayers().stream()
                .filter(p -> !p.getUuid().equals(player.getUuid()))
                .min((p1, p2) -> Double.compare(p1.squaredDistanceTo(player), p2.squaredDistanceTo(player)))
                .orElse(null);

        if (closestPlayer != null) {
            Vec3d direction = new Vec3d(
                    closestPlayer.getX() - player.getX(),
                    closestPlayer.getEyeY() - player.getEyeY(),
                    closestPlayer.getZ() - player.getZ()
            ).normalize();

            Vec3d currentLook = player.getRotationVector();
            Vec3d newLook = currentLook.add(direction.subtract(currentLook).multiply(AIM_SPEED));
            player.setRotation((float)Math.toDegrees(Math.atan2(newLook.z, newLook.x)) - 90f,
                               (float)Math.toDegrees(Math.asin(newLook.y)));
        }
    }
}
