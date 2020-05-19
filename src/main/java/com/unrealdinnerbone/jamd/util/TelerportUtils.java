package com.unrealdinnerbone.jamd.util;

import com.unrealdinnerbone.jamd.JAVD;
import com.unrealdinnerbone.jamd.JAVDRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class TelerportUtils {

    public static void teleportEntity(Entity entity, DimensionType type, BlockPos blockPos) {
        teleportEntity(entity, type, blockPos.getX() + 0.5, blockPos.getY() + 0.75, blockPos.getZ() + 0.5);
    }

    public static void teleportEntity(Entity entity, DimensionType type, double x, double y, double z) {
        SimpleTeleporter simpleTeleporter = new SimpleTeleporter(x, y, z);
        entity.changeDimension(type, simpleTeleporter);
    }

    public static void toOverworld(PlayerEntity playerEntity) {
        if(playerEntity.world instanceof ServerWorld) {
            BlockPos theBlockPos = PlayerSaveData.get((ServerWorld) playerEntity.world).getPlayersSpawnLocation(playerEntity.getUniqueID());
            if(theBlockPos == null) {
                theBlockPos = playerEntity.getBedLocation(DimensionType.OVERWORLD);
                if(theBlockPos == null) {
                    theBlockPos = playerEntity.getServer().getWorld(DimensionType.OVERWORLD).getSpawnPoint();
                }

            }
            teleportEntity(playerEntity, DimensionType.OVERWORLD, theBlockPos);
        }
    }

    public static void toVoid(PlayerEntity playerEntity) {
        toVoid(playerEntity, playerEntity.getUniqueID());

    }
    public static void toVoid(PlayerEntity playerEntity, UUID uuid) {
        if(playerEntity.world instanceof ServerWorld) {
            DimensionType TYPE = JAVD.TYPE.apply(uuid);
            ServerWorld serverWorld = (ServerWorld) playerEntity.world;
            World voidWorld = getVoidWorld(serverWorld.getServer(), TYPE);
            BlockPos theBlockPos = LocationSaveData.get(serverWorld).findPortalLocationForPlayer(uuid);
            BlockPos blockPos = theBlockPos.down();
            BlockState portalBlockState = voidWorld.getBlockState(blockPos);
            if (portalBlockState.isAir()) {
                Block block = getBlock(uuid);
                voidWorld.setBlockState(blockPos, JAVDRegistry.VOID_PORTAL_BLOCK.get().getDefaultState());
                int range = JAVD.PLATFORM_RANGE.get();
                BlockPos.getAllInBox(blockPos.add(range, 0, range), blockPos.add(-range, 0, -range)).forEach(blockPos1 -> {
                    if (voidWorld.getBlockState(blockPos1).isAir(voidWorld, blockPos1)) {
                        voidWorld.setBlockState(blockPos1, block.getDefaultState());
                    }
                });
            }
            if(playerEntity.world.dimension.getType().getModType() != JAVDRegistry.VOID.get()) {
                PlayerSaveData.get(serverWorld).setPlayersSpawnLocation(uuid, new BlockPos(playerEntity.getPosX(), playerEntity.getPosY(), playerEntity.getPosZ()));
            }
            teleportEntity(playerEntity, TYPE, findSaveBlockPos(voidWorld, theBlockPos));

        }
    }


    private static BlockPos findSaveBlockPos(World world, BlockPos blockPos) {
        if(world.isAirBlock(blockPos)) {
            return blockPos;
        }else {
            return findSaveBlockPos(world, blockPos.up());
        }
    }

    private static Block getBlock(UUID uuid) {
        return EasterEggs.getPlayersBlock(uuid).orElse(getRandom(JAVD.GENERATOR_BLOCKS.getAllElements()).orElse(Blocks.STONE));
    }


    private static World getVoidWorld(MinecraftServer minecraftServer, DimensionType TYPE) {
        return minecraftServer.getWorld(TYPE);
    }

    private static <E> Optional<E> getRandom(Collection<E> e) {
        return e.stream().skip((int) (e.size() * Math.random())).findFirst();
    }

}
