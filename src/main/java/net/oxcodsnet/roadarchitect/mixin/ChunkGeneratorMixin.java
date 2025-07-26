//package net.oxcodsnet.roadarchitect.mixin;
//
//import net.minecraft.registry.entry.RegistryEntry;
//import net.minecraft.world.gen.chunk.ChunkGenerator;
//import net.minecraft.world.gen.chunk.placement.StructurePlacement;
//import net.minecraft.world.gen.structure.Structure;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.ModifyVariable;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Пример миксина сервера.
// * <p>Example server mixin.</p>
// */
//@Mixin(ChunkGenerator.class)
//public abstract class ChunkGeneratorMixin {
//    @ModifyVariable(
//            method = "locateStructure(Lnet/minecraft/server/world/ServerWorld;"
//                    + "Lnet/minecraft/registry/entry/RegistryEntryList;"
//                    + "Lnet/minecraft/util/math/BlockPos;IZ)"
//                    + "Lcom/mojang/datafixers/util/Pair;",
//            at = @At(value = "STORE", ordinal = 0)
//    )
//    private Map<StructurePlacement, Set<RegistryEntry<Structure>>> replaceMap(Map<StructurePlacement, Set<RegistryEntry<Structure>>> original) {
//        return new ConcurrentHashMap<>();
//    }
//}
//
