package pl.polskieprawo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PolskiePrawoMod implements ModInitializer {
    private static final int TAX_INTERVAL_TICKS = 20 * 60 * 30;
    private static final Map<UUID, Integer> TAX_CLOCK = new HashMap<>();
    private static final Map<UUID, Boolean> WAS_IN_NETHER = new HashMap<>();
    private static final Map<UUID, Boolean> PERMIT_MINING = new HashMap<>();
    private static final Map<UUID, Boolean> PERMIT_LOGGING = new HashMap<>();

    @Override
    public void onInitialize() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (world.dimension() == Level.OVERWORLD && isProtectedNaturalBlock(world.getBlockState(pos))
                    && !hasPermit(sp, PERMIT_MINING)) {
                sp.displayClientMessage(Component.literal("§cINSPEKCJA OCHRONY PRZYRODY: brak pozwolenia!"), true);
                fine(sp, 12, "Nielegalne niszczenie przyrody");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (world.dimension() == Level.OVERWORLD && state.is(Blocks.OAK_LOG)
                    && !hasPermit(sp, PERMIT_LOGGING)) {
                sp.displayClientMessage(Component.literal("§cWycinka zakazana. Znajdź martwe drewno lub uzyskaj pozwolenie w urzędzie."), true);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tickServer);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof AbstractFish fish && !fish.level().isClientSide()
                    && fish.level().dimension() == Level.OVERWORLD) {
                // Fish death is reported after the event; this warning is intentionally informational.
                fish.level().players().stream().filter(p -> p instanceof ServerPlayer)
                        .map(p -> (ServerPlayer) p).filter(p -> p.distanceToSqr(fish) < 48 * 48)
                        .forEach(p -> p.displayClientMessage(Component.literal("§cUwaga: zabijanie ryb jest nielegalne! Wędkuj zamiast tego."), true));
            }
        });
    }

    private void tickServer(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean netherNow = player.level().dimension() == Level.NETHER;
            boolean netherBefore = WAS_IN_NETHER.getOrDefault(player.getUUID(), netherNow);
            if (netherBefore && !netherNow) {
                confiscateTwelvePercent(player);
                player.displayClientMessage(Component.literal("§6Urząd Celny: pobrano 12% przewożonych przedmiotów z Netheru."), false);
            }
            WAS_IN_NETHER.put(player.getUUID(), netherNow);

            int ticks = TAX_CLOCK.getOrDefault(player.getUUID(), 0) + 1;
            if (ticks >= TAX_INTERVAL_TICKS) {
                TAX_CLOCK.put(player.getUUID(), 0);
                collectLuxuryTax(player);
            } else {
                TAX_CLOCK.put(player.getUUID(), ticks);
            }
        }
    }

    private static boolean isProtectedNaturalBlock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COAL_ORE) || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.COPPER_ORE) || state.is(Blocks.GOLD_ORE) || state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.LAPIS_ORE);
    }

    private static boolean hasPermit(ServerPlayer player, Map<UUID, Boolean> permitMap) {
        return permitMap.getOrDefault(player.getUUID(), false);
    }

    private static void fine(ServerPlayer player, int amount, String reason) {
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && removed < amount; i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().removeItem(i, 1);
                removed++;
            }
        }
        player.displayClientMessage(Component.literal("§cMANDAT: " + reason + ". Skonfiskowano " + removed + " przedmiotów."), false);
    }

    private static void confiscateTwelvePercent(ServerPlayer player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) total += player.getInventory().getItem(i).getCount();
        }
        int target = (int) Math.ceil(total * 0.12);
        for (int i = 0; i < player.getInventory().getContainerSize() && target > 0; i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                int take = Math.min(target, stack.getCount());
                stack.shrink(take);
                target -= take;
            }
        }
    }

    private static void collectLuxuryTax(ServerPlayer player) {
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(Items.NETHERITE_INGOT)
                    || stack.is(Items.GOLD_INGOT)) {
                int take = Math.max(1, stack.getCount() / 10);
                take = Math.min(take, stack.getCount());
                stack.shrink(take);
                removed += take;
            }
        }
        player.displayClientMessage(Component.literal("§6Kontrola skarbowa: podatek od dóbr luksusowych pobrany. Przedmioty: " + removed), false);
    }
}
