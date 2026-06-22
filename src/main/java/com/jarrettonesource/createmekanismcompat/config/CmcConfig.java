package com.jarrettonesource.createmekanismcompat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CmcConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_MOUNTED_DIGITAL_MINER;
    public static final ModConfigSpec.BooleanValue ENABLE_MOUNTED_TELEPORTER_TARGETS;
    public static final ModConfigSpec.BooleanValue ENABLE_MOUNTED_DIMENSIONAL_STABILIZER;
    public static final ModConfigSpec.BooleanValue ENABLE_MOUNTED_WIND_GENERATOR;
    public static final ModConfigSpec.BooleanValue ENABLE_MOUNTED_LASERS;
    public static final ModConfigSpec.BooleanValue ENABLE_MOUNTED_MACHINE_SOUNDS;
    public static final ModConfigSpec.IntValue DIGITAL_MINER_SCAN_BUDGET;
    public static final ModConfigSpec.IntValue DIGITAL_MINER_SCAN_TIME_BUDGET_MICROS;
    public static final ModConfigSpec.IntValue DIGITAL_MINER_MAX_TARGET_QUEUE;
    public static final ModConfigSpec.IntValue DIGITAL_MINER_TRAIL_SAMPLES;
    public static final ModConfigSpec.IntValue DIGITAL_MINER_MAX_TICKET_CHUNKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("mounted_mekanism");

        ENABLE_MOUNTED_DIGITAL_MINER = builder
                .comment("Allow Digital Miners inside Sable sub-levels to scan and mine from the ship's projected world position.")
                .define("enableMountedDigitalMiner", true);

        ENABLE_MOUNTED_TELEPORTER_TARGETS = builder
                .comment("Project mounted Teleporter destination positions out of the Sable sub-level into global world coordinates.")
                .define("enableMountedTeleporterTargets", true);

        ENABLE_MOUNTED_DIMENSIONAL_STABILIZER = builder
                .comment("Project Dimensional Stabilizer chunk tickets from sub-level plot coordinates into the moving global chunk grid.")
                .define("enableMountedDimensionalStabilizer", true);

        ENABLE_MOUNTED_WIND_GENERATOR = builder
                .comment("Use the projected world position for Wind Generator sky and height checks inside Sable sub-levels.")
                .define("enableMountedWindGenerator", true);

        ENABLE_MOUNTED_LASERS = builder
                .comment("Keep Mekanism Laser ray hits and beam particles in the correct coordinate space when crossing Sable sub-level boundaries.")
                .define("enableMountedLasers", true);

        ENABLE_MOUNTED_MACHINE_SOUNDS = builder
                .comment("Use Sable-aware distance checks for Mekanism machine sounds inside mounted sub-levels.")
                .define("enableMountedMachineSounds", true);

        DIGITAL_MINER_SCAN_BUDGET = builder
                .comment("Maximum world positions a mounted Digital Miner inspects each scan pass before yielding.")
                .defineInRange("digitalMinerScanBudget", 16_384, 32, 262_144);

        DIGITAL_MINER_SCAN_TIME_BUDGET_MICROS = builder
                .comment("Soft wall-clock time budget, in microseconds, for one mounted Digital Miner scan pass.")
                .defineInRange("digitalMinerScanTimeBudgetMicros", 12_000, 1_000, 50_000);

        DIGITAL_MINER_MAX_TARGET_QUEUE = builder
                .comment("Maximum accepted world targets a mounted Digital Miner keeps queued for mining and the visible To Mine count.")
                .defineInRange("digitalMinerMaxTargetQueue", 32_768, 1, 262_144);

        DIGITAL_MINER_TRAIL_SAMPLES = builder
                .comment("Maximum interpolation samples added when a mounted Digital Miner moves between mining attempts.")
                .defineInRange("digitalMinerTrailSamples", 8, 1, 64);

        DIGITAL_MINER_MAX_TICKET_CHUNKS = builder
                .comment("Maximum global chunks the mounted Digital Miner asks Mekanism's chunk loader to keep loaded. The default covers a radius-30 miner's horizontal scan footprint.")
                .defineInRange("digitalMinerMaxTicketChunks", 25, 1, 81);

        builder.pop();
        SPEC = builder.build();
    }

    private CmcConfig() {
    }
}
