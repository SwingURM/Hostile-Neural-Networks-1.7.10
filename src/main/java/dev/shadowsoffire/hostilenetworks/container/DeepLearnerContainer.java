package dev.shadowsoffire.hostilenetworks.container;

import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.item.HostileItems;
import dev.shadowsoffire.hostilenetworks.util.Constants;

/**
 * Container for the Deep Learner GUI.
 * Allows managing the 4 data model slots in a Deep Learner item.
 *
 * The Deep Learner stores entity IDs in NBT, but this container shows
 * the actual DataModelItem stacks. When items are placed/removed,
 * they are synced to/from the Deep Learner's NBT.
 */
public class DeepLearnerContainer extends Container {

    private static final int DEEP_LEARNER_SLOTS = Constants.DEEP_LEARNER_SLOTS;
    private final EntityPlayer player;
    private final DeepLearnerInventory deepLearnerInv;
    private Consumer<Integer> notifyCallback;

    public DeepLearnerContainer(InventoryPlayer playerInventory, EntityPlayer player) {
        this.player = player;
        this.deepLearnerInv = new DeepLearnerInventory(player);

        // Add Deep Learner data model slots - match original positions
        // Slot 0: 256, 99
        addSlotToContainer(new DeepLearnerSlot(deepLearnerInv, 0, 256, 99));
        // Slot 1: 274, 99
        addSlotToContainer(new DeepLearnerSlot(deepLearnerInv, 1, 274, 99));
        // Slot 2: 256, 117
        addSlotToContainer(new DeepLearnerSlot(deepLearnerInv, 2, 256, 117));
        // Slot 3: 274, 117
        addSlotToContainer(new DeepLearnerSlot(deepLearnerInv, 3, 274, 117));

        // Add player inventory slots (x=89, y=153 + row*18)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                int x = 89 + col * 18;
                int y = 153 + row * 18;
                addSlotToContainer(new Slot(playerInventory, slotIndex, x, y));
            }
        }

        // Add hotbar slots (x=89, y=211)
        for (int col = 0; col < 9; col++) {
            int slotIndex = col;
            int x = 89 + col * 18;
            int y = 211;
            addSlotToContainer(new Slot(playerInventory, slotIndex, x, y));
        }
    }

    /**
     * Set a callback to be notified when a slot changes.
     */
    public void setNotifyCallback(Consumer<Integer> callback) {
        this.notifyCallback = callback;
    }

    /**
     * Notify the callback that a slot has changed.
     */
    private void onSlotChanged(int slotId) {
        if (notifyCallback != null) {
            notifyCallback.accept(slotId);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        // Check if player has a Deep Learner in hand or offhand
        ItemStack mainHand = player.getHeldItem();
        ItemStack offHand = player.getEquipmentInSlot(1);
        return (mainHand != null && mainHand.getItem() instanceof DeepLearnerItem)
            || (offHand != null && offHand.getItem() instanceof DeepLearnerItem);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        // Sync data back to NBT when GUI closes
        deepLearnerInv.syncToNBT();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        Slot slot = getSlot(slotIndex);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }

        ItemStack srcStack = slot.getStack();
        ItemStack copyStack = srcStack.copy();
        copyStack.stackSize = 1;

        boolean moved = false;

        // From Deep Learner (0-3) to player inventory
        if (slotIndex < DEEP_LEARNER_SLOTS) {
            moved = mergeItemStack(copyStack, DEEP_LEARNER_SLOTS, this.inventorySlots.size(), true);
        }
        // From player inventory to Deep Learner (only DataModelItem, not BlankDataModelItem)
        else {
            if (srcStack.getItem() instanceof DataModelItem) {
                moved = mergeItemStack(copyStack, 0, DEEP_LEARNER_SLOTS, true);
            } else {
                if (!mergeItemStack(copyStack, this.inventorySlots.size() - 9, this.inventorySlots.size(), true)) {
                    if (!mergeItemStack(copyStack, DEEP_LEARNER_SLOTS, this.inventorySlots.size() - 9, true)) {
                        return null;
                    }
                    moved = true;
                } else {
                    moved = true;
                }
            }
        }

        if (!moved) {
            return null;
        }

        if (copyStack.stackSize <= 0) {
            return null;
        }

        return copyStack;
    }

    /**
     * Slot specifically for Deep Learner - only accepts DataModel items (not blank).
     */
    public class DeepLearnerSlot extends Slot {

        public DeepLearnerSlot(DeepLearnerInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            // Only DataModelItem can be placed here, not BlankDataModelItem
            return stack != null && stack.getItem() instanceof DataModelItem;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            // Notify the callback when slot changes
            DeepLearnerContainer.this.onSlotChanged(getSlotIndex());
        }
    }

    /**
     * Inventory for the Deep Learner.
     * Stores the actual ItemStacks and syncs entity IDs back to NBT.
     */
    public static class DeepLearnerInventory implements net.minecraft.inventory.IInventory {

        private final ItemStack[] stacks = new ItemStack[4];
        private final EntityPlayer player;

        public DeepLearnerInventory(EntityPlayer player) {
            this.player = player;
            // Initialize from NBT
            loadFromNBT();
        }

        /**
         * Load entity IDs from the Deep Learner's NBT and create placeholder items.
         */
        private void loadFromNBT() {
            // Find the Deep Learner stack
            ItemStack deepLearner = player.getHeldItem();
            if (deepLearner == null || !(deepLearner.getItem() instanceof DeepLearnerItem)) {
                deepLearner = player.getEquipmentInSlot(1);
                if (deepLearner == null || !(deepLearner.getItem() instanceof DeepLearnerItem)) {
                    return;
                }
            }

            // Read entity IDs from NBT
            if (deepLearner.hasTagCompound() && deepLearner.getTagCompound()
                .hasKey("Models")) {
                NBTTagList list = deepLearner.getTagCompound()
                    .getTagList("Models", 10);
                for (int i = 0; i < 4 && i < list.tagCount(); i++) {
                    NBTTagCompound slotTag = list.getCompoundTagAt(i);
                    String entityId = slotTag.getString("id");
                    if (entityId != null && !entityId.isEmpty()) {
                        // Create a data model item stack
                        stacks[i] = new ItemStack(HostileItems.data_model);
                        if (!stacks[i].hasTagCompound()) {
                            stacks[i].setTagCompound(new NBTTagCompound());
                        }
                        stacks[i].getTagCompound()
                            .setString("EntityId", entityId);
                        // Load CurrentData and Iterations from NBT
                        int currentData = slotTag.getInteger("CurrentData");
                        int iterations = slotTag.getInteger("Iterations");
                        stacks[i].getTagCompound()
                            .setInteger("CurrentData", currentData);
                        stacks[i].getTagCompound()
                            .setInteger("Iterations", iterations);
                        // Restore damage bar from data value
                        DataModelItem.restoreDamage(stacks[i]);
                    }
                }
            }
        }

        @Override
        public int getSizeInventory() {
            return 4;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stacks[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            if (stacks[slot] != null) {
                ItemStack stack = stacks[slot];
                ItemStack result = stack.splitStack(amount);
                if (stack.stackSize <= 0) {
                    stacks[slot] = null;
                }
                return result;
            }
            return null;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            ItemStack stack = stacks[slot];
            stacks[slot] = null;
            return stack;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            stacks[slot] = stack;
            if (stack != null && stack.stackSize > getInventoryStackLimit()) {
                stack.stackSize = getInventoryStackLimit();
            }
        }

        @Override
        public String getInventoryName() {
            return "container.hostilenetworks.deep_learner";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return true;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            syncToNBT();
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {
            syncToNBT();
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            // Only DataModelItem can be placed here (including blank models with damage=0)
            return stack != null && stack.getItem() instanceof DataModelItem;
        }

        /**
         * Sync the inventory state back to the Deep Learner's NBT.
         */
        public void syncToNBT() {
            // Find the current Deep Learner stack
            ItemStack target = player.getHeldItem();
            if (target == null || !(target.getItem() instanceof DeepLearnerItem)) {
                target = player.getEquipmentInSlot(1);
                if (target == null || !(target.getItem() instanceof DeepLearnerItem)) {
                    return;
                }
            }

            // Get or create the models list
            if (!target.hasTagCompound()) {
                target.setTagCompound(new NBTTagCompound());
            }
            NBTTagCompound tag = target.getTagCompound();
            NBTTagList list;
            if (tag.hasKey("Models")) {
                list = tag.getTagList("Models", 10);
            } else {
                list = new NBTTagList();
                tag.setTag("Models", list);
            }

            // Sync each slot
            for (int i = 0; i < 4; i++) {
                // Ensure list has enough elements
                while (list.tagCount() <= i) {
                    NBTTagCompound emptyTag = new NBTTagCompound();
                    emptyTag.setString("id", "");
                    emptyTag.setInteger("CurrentData", 0);
                    emptyTag.setInteger("Iterations", 0);
                    list.appendTag(emptyTag);
                }

                NBTTagCompound slotTag = list.getCompoundTagAt(i);
                if (stacks[i] != null && stacks[i].hasTagCompound()) {
                    String entityId = stacks[i].getTagCompound()
                        .getString("EntityId");
                    int currentData = stacks[i].getTagCompound()
                        .getInteger("CurrentData");
                    int iterations = stacks[i].getTagCompound()
                        .getInteger("Iterations");
                    slotTag.setString("id", entityId != null ? entityId : "");
                    slotTag.setInteger("CurrentData", currentData);
                    slotTag.setInteger("Iterations", iterations);
                } else {
                    slotTag.setString("id", "");
                    slotTag.setInteger("CurrentData", 0);
                    slotTag.setInteger("Iterations", 0);
                }
            }
        }
    }
}
