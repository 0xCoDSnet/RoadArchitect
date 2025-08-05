package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature that places road segments stored in {@link RoadBuilderStorage}.
 *
 * Толстая линия реализована через проверку расстояния от клетки до центральной
 * прямой (|dx · dir.z − dz · dir.x| ≤ halfWidth). Такой подход избегает
 * «шахматных» дыр на диагоналях :contentReference[oaicite:2]{index=2}.
 */
public final class RoadFeature extends Feature<RoadFeatureConfig> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadFeature");

    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<RoadFeatureConfig> ctx) {
        // ─── Проверяем, что мы на сервере, а не в data-gen ───
        ServerWorld serverWorld = ctx.getWorld().toServerWorld();
        if (serverWorld == null) {
            return false;
        }

        StructureWorldAccess world = ctx.getWorld();
        ChunkPos chunk = new ChunkPos(ctx.getOrigin());

        RoadBuilderStorage builder  = RoadBuilderStorage.get(serverWorld);
        PathStorage         paths    = PathStorage.get(serverWorld);

        List<RoadBuilderStorage.SegmentEntry> queue =
                new ArrayList<>(builder.getSegments(chunk));
        if (queue.isEmpty()) {
            return false;
        }

        int        halfWidth = Math.max(0, ctx.getConfig().orthWidth() / 2);
        BlockState roadState = Blocks.COBBLESTONE.getDefaultState();
        boolean    placedAny = false;

        // ─── Проходим все сегменты, привязанные к текущему чанку ───
        for (RoadBuilderStorage.SegmentEntry entry : queue) {
            String[] ids = entry.pathKey().split("\\|", 2);
            if (ids.length != 2) {
                LOGGER.warn("Malformed path key '{}'; skipping", entry.pathKey());
                builder.removeSegment(chunk, entry);
                continue;
            }

            List<BlockPos> pts = paths.getPath(ids[0], ids[1]);
            if (pts.isEmpty()) {
                builder.removeSegment(chunk, entry);
                continue;
            }

            int from = Math.max(0, entry.start());
            int to   = Math.min(pts.size(), entry.end());

            // ─── Основной проход по узлам ───
            for (int i = from; i < to; i++) {
                BlockPos p = pts.get(i);

                // направление вычисляем по узлам i-2 и i+2 (как в SettlementRoads)
                int       prevIdx = Math.max(0, i - 2);
                int       nextIdx = Math.min(pts.size() - 1, i + 2);
                BlockPos  prev    = pts.get(prevIdx);
                BlockPos  next    = pts.get(nextIdx);

                Vec3d delta = new Vec3d(
                        next.getX() - prev.getX(),
                        0.0D,
                        next.getZ() - prev.getZ()
                );

                // fallback на соседний узел, если delta == (0,0,0)
                if (delta.lengthSquared() == 0.0D && i > 0) {
                    BlockPos prev2 = pts.get(i - 1);
                    delta = new Vec3d(
                            p.getX() - prev2.getX(),
                            0.0D,
                            p.getZ() - prev2.getZ()
                    );
                }

                Vec3d dir = delta.normalize();   // единичный вектор вдоль дороги
                double nx = dir.x;
                double nz = dir.z;

                // ─── «Толстый» отрезок ───
                boolean diagonal = Math.abs(nx) > 0.001 && Math.abs(nz) > 0.001;

                for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                    for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                        double dist = Math.abs(dx * nz - dz * nx);          // прежняя формула
                        boolean insideStrip  = dist <= halfWidth + 0.01;    // как было
                        boolean makeChunkier = diagonal &&                  // добавляем углы
                                Math.max(Math.abs(dx), Math.abs(dz)) <= halfWidth;

                        if (insideStrip || makeChunkier) {
                            BlockPos roadPos = p.add(dx, -1, dz);
                            placeRoad(world, roadPos, roadState);
                        }
                    }
                }
            }

            builder.removeSegment(chunk, entry);
            placedAny = true;
        }

        return placedAny;
    }

    /**
     * Устанавливает блок дороги и очищает воздух над ним.
     *
     * @param world     Мир/чанк для записи
     * @param pos       Координата нижнего блока дороги
     * @param stateRoad Состояние блока дороги
     */
    private static void placeRoad(StructureWorldAccess world, BlockPos pos, BlockState stateRoad) {
        world.setBlockState(pos, stateRoad, Block.NOTIFY_LISTENERS);
        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
    }
}

