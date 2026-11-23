package com.example.cropreplant;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ToolboxMenu extends ChestMenu {

    private static final int SEED_SLOT = 0;
    private static final int BED_SLOT = 1;

    public ToolboxMenu(int id, Inventory playerInventory, Player viewer) {
        super(MenuType.GENERIC_9x3, id, playerInventory, new SimpleContainer(27), 3);
        Container inv = getContainer();
        inv.setItem(SEED_SLOT, seedIcon(CropReplantMod.isEnabled(viewer)));
        inv.setItem(BED_SLOT, bedIcon());
    }

    private static ItemStack seedIcon(boolean enabled) {
        ItemStack stack = new ItemStack(Items.WHEAT_SEEDS);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal(enabled ? "§a补种：开启（点击关闭）" : "§c补种：关闭（点击开启）"));
        return stack;
    }

    private static ItemStack bedIcon() {
        ItemStack stack = new ItemStack(Items.BED.red());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("§b设置出生点（点击设置到脚下）"));
        return stack;
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (slot == SEED_SLOT) {
            boolean enabled = CropReplantMod.toggleFor(player);
            getContainer().setItem(SEED_SLOT, seedIcon(enabled));
            broadcastChanges();
            return;
        }
        if (slot == BED_SLOT) {
            if (player instanceof ServerPlayer serverPlayer) {
                CropReplantMod.setHome(serverPlayer);
            }
            return;
        }
        super.clicked(slot, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
