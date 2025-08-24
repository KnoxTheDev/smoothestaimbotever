package com.example.smoothestaimbotever;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import com.example.smoothestaimbotever.mixin.EntityAccessorMixin;

import java.util.List;
import java.util.Comparator;
import java.util.stream.StreamSupport;
import java.lang.Iterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

public class ExampleMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("smoothestaimbotever");
    private final MinecraftClient client = MinecraftClient.getInstance();

    private boolean enabled = false; // Aimbot toggle

    // Hyper-optimized aimbot config
    private static class AimbotConfig {
        public String targets = "player"; // player/crystals
        public String mode = "both"; // horizontal/vertical/both
        public int visibleTime = 50; // ms
        public float smoothing = 0.5f; // 0.1 - 1.0
        public float fov = 50.0f; // degrees
        public float range = 5.0f; // blocks
        public float random = 0.1f; // 0.1 - 1.0
        public String hitbox = "eye"; // eye/center/bottom
    }
    private final AimbotConfig config = new AimbotConfig();

    @Override
    public void onInitializeClient() {
        LOGGER.info("smoothest aimbot ever for block game initialised.");

        // Register /aimbot commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ClientCommandManager.literal("aimbot")
                .executes(context -> {
                    listConfig();
                    return 1;
                })
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        listConfig();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        enabled = true;
                        LOGGER.info("Aimbot enabled.");
                        return 1;
                    }))
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        enabled = false;
                        LOGGER.info("Aimbot disabled.");
                        return 1;
                    }))
                // Existing config commands
                .then(ClientCommandManager.literal("targets")
                    .then(ClientCommandManager.argument("value", StringArgumentType.word())
                        .executes(context -> {
                            config.targets = StringArgumentType.getString(context, "value");
                            LOGGER.info("Aimbot targets set to " + config.targets);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("mode")
                    .then(ClientCommandManager.argument("value", StringArgumentType.word())
                        .executes(context -> {
                            config.mode = StringArgumentType.getString(context, "value");
                            LOGGER.info("Aimbot mode set to " + config.mode);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("visibleTime")
                    .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(0, 500))
                        .executes(context -> {
                            config.visibleTime = IntegerArgumentType.getInteger(context, "value");
                            LOGGER.info("Aimbot visibleTime set to " + config.visibleTime);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("smoothing")
                    .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 1.0f))
                        .executes(context -> {
                            config.smoothing = FloatArgumentType.getFloat(context, "value");
                            LOGGER.info("Aimbot smoothing set to " + config.smoothing);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("fov")
                    .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 360.0f))
                        .executes(context -> {
                            config.fov = FloatArgumentType.getFloat(context, "value");
                            LOGGER.info("Aimbot FOV set to " + config.fov);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("range")
                    .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 10.0f))
                        .executes(context -> {
                            config.range = FloatArgumentType.getFloat(context, "value");
                            LOGGER.info("Aimbot range set to " + config.range);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("random")
                    .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 1.0f))
                        .executes(context -> {
                            config.random = FloatArgumentType.getFloat(context, "value");
                            LOGGER.info("Aimbot randomization set to " + config.random);
                            return 1;
                        })))
                .then(ClientCommandManager.literal("hitbox")
                    .then(ClientCommandManager.argument("value", StringArgumentType.word())
                        .executes(context -> {
                            config.hitbox = StringArgumentType.getString(context, "value");
                            LOGGER.info("Aimbot hitbox set to " + config.hitbox);
                            return 1;
                        })));
        });

        // Rotation-only aimbot loop
        new Thread(() -> {
            while (true) {
                try {
                    ClientPlayerEntity player = client.player;
                    if (enabled && player != null && client.world != null) {
                        aimAtTarget(player);
                    }
                    Thread.sleep(5);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void listConfig() {
        LOGGER.info("Aimbot Config: enabled=" + enabled + " targets=" + config.targets + " mode=" + config.mode +
            " visibleTime=" + config.visibleTime + " smoothing=" + config.smoothing + " fov=" + config.fov +
            " range=" + config.range + " random=" + config.random + " hitbox=" + config.hitbox);
    }

    private void aimAtTarget(ClientPlayerEntity player) {
        Iterable<Entity> entityIterable = client.world.getEntities();
        List<Entity> entities = StreamSupport.stream(entityIterable.spliterator(), false)
                .filter(e -> e != player)
                .filter(this::isTargetValid)
                .filter(this::hasLineOfSight)
                .toList();

        if (entities.isEmpty()) return;

        Entity closest = entities.stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo))
                .orElse(null);

        if (closest == null) return;

        Vec3d targetPos = getTargetPosition(closest);
        Vec3d dir = targetPos.subtract(player.getPos()).normalize();

        Vec3d look = player.getRotationVector();
        Vec3d newLook = look.add(dir.subtract(look).multiply(config.smoothing));

        // Add random jitter
        newLook = new Vec3d(
            newLook.x + (Math.random() - 0.5) * config.random,
            newLook.y + (Math.random() - 0.5) * config.random,
            newLook.z + (Math.random() - 0.5) * config.random
        );

        ((EntityAccessorMixin) player).callSetRotation(
            (float) Math.toDegrees(Math.atan2(newLook.z, newLook.x)) - 90f,
            (float) Math.toDegrees(Math.asin(newLook.y))
        );
    }

    private boolean isTargetValid(Entity e) {
        if (!e.isAlive()) return false;
        if (config.targets.equals("player") && !(e instanceof PlayerEntity)) return false;
        // Add crystal logic if desired
        double distance = client.player.squaredDistanceTo(e);
        if (distance > config.range * config.range) return false;

        Vec3d dir = getTargetPosition(e).subtract(client.player.getPos()).normalize();
        float angle = (float) Math.toDegrees(Math.acos(client.player.getRotationVector().dotProduct(dir)));
        return angle <= config.fov / 2.0f;
    }

    private Vec3d getTargetPosition(Entity e) {
        switch (config.hitbox) {
            case "eye": return e.getEyePos();
            case "center": return e.getPos().add(0, e.getHeight() / 2.0, 0);
            case "bottom": return e.getPos();
            default: return e.getEyePos();
        }
    }

    private boolean hasLineOfSight(Entity target) {
        Vec3d start = client.player.getEyePos();
        Vec3d end = getTargetPosition(target);
        return client.world.raycast(new net.minecraft.world.RaycastContext(
                start, end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                client.player)).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }
}
