package com.example.cropreplant;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ToolboxMenu extends ChestMenu {

    private static final int[] TOGGLE_SLOTS = {0, 1, 2};

    public ToolboxMenu(int id, Inventory playerInventory, Player viewer) {
        super(MenuType.GENERIC_9x6, id, playerInventory, new SimpleContainer(54), 6);
        Container inv = getContainer();
        boolean enabled = CropReplantMod.isEnabled(viewer);
        Item[] icons = {Items.WHEAT_SEEDS, Items.BED.red(), Items.PITCHER_PLANT};
        for (int i = 0; i < icons.length; i++) {
            inv.setItem(TOGGLE_SLOTS[i], CropReplantMod.toggleIcon(icons[i], enabled));
        }
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        for (int toggleSlot : TOGGLE_SLOTS) {
            if (slot == toggleSlot) {
                boolean enabled = CropReplantMod.toggleFor(player);
                for (int s : TOGGLE_SLOTS) {
                    getContainer().setItem(s, CropReplantMod.toggleIcon(getContainer().getItem(s).getItem(), enabled));
                }
                broadcastChanges();
                return;
            }
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
