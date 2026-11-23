package com.example.cropreplant;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ToolboxMenu extends ChestMenu {

    private final Player viewer;

    public ToolboxMenu(int id, Inventory playerInventory, Player viewer) {
        super(MenuType.GENERIC_9x1, id, playerInventory, new SimpleContainer(9), 1);
        this.viewer = viewer;
        Container inv = getContainer();
        inv.setItem(0, CropReplantMod.toggleIcon(CropReplantMod.isEnabled(viewer)));
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (slot == 0) {
            boolean enabled = CropReplantMod.toggleFor(player);
            getContainer().setItem(0, CropReplantMod.toggleIcon(enabled));
            broadcastChanges();
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
