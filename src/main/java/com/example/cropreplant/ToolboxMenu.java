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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ToolboxMenu extends ChestMenu {

    private static final int SEED_SLOT = 0;
    private static final int BED_SLOT = 1;
    private static final int MILK_SLOT = 2;
    private static final int STAR_SLOT = 3;

    public ToolboxMenu(int id, Inventory playerInventory, Player viewer) {
        super(MenuType.GENERIC_9x3, id, playerInventory, new SimpleContainer(27), 3);
        Container inv = getContainer();
        inv.setItem(SEED_SLOT, seedIcon(CropReplantMod.isEnabled(viewer)));
        inv.setItem(BED_SLOT, named(Items.BED.red(), "§b设置出生点（点击设置到脚下）"));
        inv.setItem(MILK_SLOT, named(Items.MILK_BUCKET, "§d清除 buff（点击清掉所有状态效果）"));
        inv.setItem(STAR_SLOT, named(Items.NETHER_STAR, "§c杀死自己（点击 ykill）"));
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack seedIcon(boolean enabled) {
        return named(Items.WHEAT_SEEDS,
                enabled ? "§a补种：开启（点击关闭）" : "§c补种：关闭（点击开启）");
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (slot == SEED_SLOT) {
            boolean enabled = CropReplantMod.toggleFor(player);
            getContainer().setItem(SEED_SLOT, seedIcon(enabled));
            broadcastChanges();
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (slot == BED_SLOT) {
            CropReplantMod.setHome(sp);
            return;
        }
        if (slot == MILK_SLOT) {
            sp.removeAllEffects();
            sp.sendSystemMessage(Component.literal("已清除你身上的所有状态效果"));
            return;
        }
        if (slot == STAR_SLOT) {
            sp.closeContainer();
            sp.kill(sp.level());
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
