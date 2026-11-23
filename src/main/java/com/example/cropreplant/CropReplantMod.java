package com.example.cropreplant;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CropReplantMod implements ModInitializer {

    public static final String MOD_ID = "crop_replant";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int UPDATE_FLAGS = 3;
    private static final int MAX_UPDATE_DEPTH = 512;

    private static final Map<BlockPos, PendingCrop> PENDING = new ConcurrentHashMap<>();
    private static final Set<UUID> DISABLED = ConcurrentHashMap.newKeySet();

    private static final class PendingCrop {
        final Block crop;
        final Item seed;

        PendingCrop(Block crop, Item seed) {
            this.crop = crop;
            this.seed = seed;
        }
    }

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.BEFORE.register(this::onBeforeBreak);
        PlayerBlockBreakEvents.AFTER.register(this::onAfterBreak);
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            dispatcher.register(Commands.literal("replant")
                    .executes(ctx -> {
                        Player p = ctx.getSource().getPlayerOrException();
                        boolean on = toggleFor(p);
                        ctx.getSource().sendSuccess(() -> Component.literal(on ? "自动补种：已开启" : "自动补种：已关闭"), false);
                        return 1;
                    }));
            dispatcher.register(Commands.literal("yueyue")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        MenuProvider provider = new SimpleMenuProvider(
                                (id, inv, player) -> new ToolboxMenu(id, inv, player),
                                Component.literal("悦悦工具箱"));
                        p.openMenu(provider);
                        return 1;
                    })
                    .then(Commands.literal("开").executes(ctx -> {
                        Player p = ctx.getSource().getPlayerOrException();
                        setEnabled(p, true);
                        ctx.getSource().sendSuccess(() -> Component.literal("自动补种：已开启"), false);
                        return 1;
                    }))
                    .then(Commands.literal("关").executes(ctx -> {
                        Player p = ctx.getSource().getPlayerOrException();
                        setEnabled(p, false);
                        ctx.getSource().sendSuccess(() -> Component.literal("自动补种：已关闭"), false);
                        return 1;
                    })));
            dispatcher.register(Commands.literal("home")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        BlockPos pos = p.blockPosition();
                        net.minecraft.resources.Identifier dim = p.level().dimension().identifier();
                        LevelData.RespawnData data = LevelData.RespawnData.of(
                                p.level().dimension(), pos, p.getYRot(), p.getXRot());
                        p.setRespawnPosition(new ServerPlayer.RespawnConfig(data, true), true);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "已设置你的出生点：" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " @ " + dim), false);
                        return 1;
                    }));
        });
        LOGGER.info("[crop_replant] loaded. Commands: /replant, /yueyue [开|关], /home");
    }

    public static boolean isEnabled(Player player) {
        return !DISABLED.contains(player.getUUID());
    }

    public static void setEnabled(Player player, boolean enabled) {
        UUID id = player.getUUID();
        if (enabled) {
            DISABLED.remove(id);
        } else {
            DISABLED.add(id);
        }
    }

    public static boolean toggleFor(Player player) {
        UUID id = player.getUUID();
        boolean enabled;
        if (DISABLED.remove(id)) {
            enabled = true;
        } else {
            DISABLED.add(id);
            enabled = false;
        }
        return enabled;
    }

    public static ItemStack toggleIcon(Item item, boolean enabled) {
        ItemStack icon = new ItemStack(item);
        icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal(enabled ? "§a自动补种：开启（点击关闭）" : "§c自动补种：关闭（点击开启）"));
        return icon;
    }

    private boolean onBeforeBreak(Level level, Player player, BlockPos pos,
                                  BlockState state, BlockEntity blockEntity) {
        if (level.isClientSide()) {
            return true;
        }
        if (!isEnabled(player)) {
            return true;
        }
        GameType mode = player.gameMode();
        if (mode.isCreative() || mode == GameType.SPECTATOR) {
            return true;
        }
        if (!isMatureCrop(state)) {
            return true;
        }
        Item seed = seedFor(state.getBlock());
        if (seed != null) {
            PENDING.put(pos.immutable(), new PendingCrop(state.getBlock(), seed));
        }
        return true;
    }

    private void onAfterBreak(Level level, Player player, BlockPos pos,
                              BlockState state, BlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }
        PendingCrop pending = PENDING.remove(pos);
        if (pending == null) {
            return;
        }
        BlockState current = level.getBlockState(pos);
        if (!current.isAir() && !current.canBeReplaced()) {
            return;
        }
        if (!consumeOne(player, pending.seed)) {
            return;
        }
        level.setBlock(pos, pending.crop.defaultBlockState(), UPDATE_FLAGS, MAX_UPDATE_DEPTH);
    }

    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
        }
        return false;
    }

    private static Item seedFor(Block block) {
        if (block == Blocks.WHEAT) {
            return Items.WHEAT_SEEDS;
        }
        if (block == Blocks.CARROTS) {
            return Items.CARROT;
        }
        if (block == Blocks.POTATOES) {
            return Items.POTATO;
        }
        if (block == Blocks.BEETROOTS) {
            return Items.BEETROOT_SEEDS;
        }
        if (block == Blocks.TORCHFLOWER_CROP) {
            return Items.TORCHFLOWER_SEEDS;
        }
        if (block == Blocks.NETHER_WART) {
            return Items.NETHER_WART;
        }
        return null;
    }

    private static boolean consumeOne(Player player, Item item) {
        ItemStack wanted = new ItemStack(item);
        int slot = player.getInventory().findSlotMatchingItem(wanted);
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return false;
        }
        return !player.getInventory().removeItem(slot, 1).isEmpty();
    }
}
