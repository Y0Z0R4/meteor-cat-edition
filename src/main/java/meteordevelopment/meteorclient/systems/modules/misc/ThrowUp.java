/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThrowUp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> itemsToThrow = sgGeneral.add(new IntSetting.Builder()
        .name("items-to-throw")
        .description("Number of items to throw every tick.")
        .defaultValue(8)
        .min(1)
        .sliderMax(36)
        .build()
    );

    private final Setting<Boolean> randomize = sgGeneral.add(new BoolSetting.Builder()
        .name("randomize")
        .description("Whether to throw random items or not.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> excludeHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("exclude-hotbar")
        .description("Whether to exclude the hotbar from being thrown.")
        .defaultValue(false)
        .build()
    );

    private final Random random = new Random();

    public ThrowUp() {
        super(Categories.Misc, "throw-up", "Throws items out of your inventory.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.interactionManager == null) return;

        for (int i = 0; i < itemsToThrow.get(); i++) {
            int slot = getRandomSlot();
            if (slot == -1) return;

            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 1, SlotActionType.THROW, mc.player);
        }
    }

    private int getRandomSlot() {
        List<Integer> validSlots = getValidSlots(excludeHotbar.get() ? 9 : 0, mc.player.getInventory().size() - 1);

        if (validSlots.isEmpty()) return -1;

        if (randomize.get()) {
            return validSlots.get(random.nextInt(validSlots.size()));
        } else {
            return validSlots.get(0);
        }
    }

    private List<Integer> getValidSlots(int start, int end) {
        List<Integer> validSlots = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                validSlots.add(i);
            }
        }
        return validSlots;
    }
}
