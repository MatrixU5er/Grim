package ac.grim.grimac.utils.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.inventory.AbstractContainerMenu;
import ac.grim.grimac.utils.inventory.slot.EquipmentSlot;
import ac.grim.grimac.utils.inventory.slot.ResultSlot;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.Getter;
import org.bukkit.GameMode;

public class Inventory extends AbstractContainerMenu {
    public static final int SLOT_OFFHAND = 45;
    public static final int HOTBAR_OFFSET = 36;
    private static final int SLOT_HELMET = 5;
    private static final int SLOT_CHESTPLATE = 6;
    private static final int SLOT_LEGGINGS = 7;
    private static final int SLOT_BOOTS = 8;
    private static final int TOTAL_SIZE = 46;
    public static final int ITEMS_START = 9;
    public static final int ITEMS_END = 45;
    public int selected = 0;
    @Getter
    InventoryStorage playerInventory;

    public Inventory(GrimPlayer player, InventoryStorage playerInventory) {
        this.playerInventory = playerInventory;

        super.setPlayer(player);
        super.setPlayerInventory(this);

        // Result slot
        addSlot(new ResultSlot(playerInventory, 0));
        // Crafting slots
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(playerInventory, i));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new EquipmentSlot(EquipmentType.byArmorID(i), playerInventory, i + 4));
        }
        // Inventory slots
        for (int i = 0; i < 9 * 4; i++) {
            addSlot(new Slot(playerInventory, i + 9));
        }
        // Offhand
        addSlot(new Slot(playerInventory, 45));
    }

    public ItemStack getHeldItem() {
        return playerInventory.getItem(selected + HOTBAR_OFFSET);
    }

    public void setHeldItem(ItemStack item) {
        playerInventory.setItem(selected + HOTBAR_OFFSET, item);
    }

    public ItemStack getOffhandItem() {
        return playerInventory.getItem(SLOT_OFFHAND);
    }

    public boolean add(ItemStack p_36055_) {
        return this.add(-1, p_36055_);
    }

    public int getFreeSlot() {
        for (int i = 0; i < playerInventory.items.length; ++i) {
            if (playerInventory.getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public int getSlotWithRemainingSpace(ItemStack toAdd) {
        if (this.hasRemainingSpaceForItem(getHeldItem(), toAdd)) {
            return this.selected;
        } else if (this.hasRemainingSpaceForItem(getOffhandItem(), toAdd)) {
            return 40;
        } else {
            for (int i = ITEMS_START; i <= ITEMS_END; ++i) {
                if (this.hasRemainingSpaceForItem(playerInventory.getItem(i), toAdd)) {
                    return i;
                }
            }

            return -1;
        }
    }

    private boolean hasRemainingSpaceForItem(ItemStack one, ItemStack two) {
        return !one.isEmpty() && ItemStack.isSameItemSameTags(one, two) && one.getAmount() < one.getMaxStackSize() && one.getAmount() < this.getMaxStackSize();
    }

    private int addResource(ItemStack resource) {
        int i = this.getSlotWithRemainingSpace(resource);
        if (i == -1) {
            i = this.getFreeSlot();
        }

        return i == -1 ? resource.getAmount() : this.addResource(i, resource);
    }

    private int addResource(int slot, ItemStack stack) {
        int i = stack.getAmount();
        ItemStack itemstack = playerInventory.getItem(slot);

        if (itemstack.isEmpty()) {
            itemstack = stack.copy();
            itemstack.setAmount(0);
            playerInventory.setItem(slot, itemstack);
        }

        int j = i;
        if (i > itemstack.getMaxStackSize() - itemstack.getAmount()) {
            j = itemstack.getMaxStackSize() - itemstack.getAmount();
        }

        if (j > this.getMaxStackSize() - itemstack.getAmount()) {
            j = this.getMaxStackSize() - itemstack.getAmount();
        }

        if (j == 0) {
            return i;
        } else {
            i = i - j;
            itemstack.grow(j);
            return i;
        }
    }

    public boolean add(int p_36041_, ItemStack p_36042_) {
        if (p_36042_.isEmpty()) {
            return false;
        } else {
            if (p_36042_.isDamaged()) {
                if (p_36041_ == -1) {
                    p_36041_ = this.getFreeSlot();
                }

                if (p_36041_ >= 0) {
                    playerInventory.setItem(p_36041_, p_36042_.copy());
                    p_36042_.setAmount(0);
                    return true;
                } else if (player.gamemode == GameMode.CREATIVE) {
                    p_36042_.setAmount(0);
                    return true;
                } else {
                    return false;
                }
            } else {
                int i;
                do {
                    i = p_36042_.getAmount();
                    if (p_36041_ == -1) {
                        p_36042_.setAmount(this.addResource(p_36042_));
                    } else {
                        p_36042_.setAmount(this.addResource(p_36041_, p_36042_));
                    }
                } while (!p_36042_.isEmpty() && p_36042_.getAmount() < i);

                if (p_36042_.getAmount() == i && player.gamemode == GameMode.CREATIVE) {
                    p_36042_.setAmount(0);
                    return true;
                } else {
                    return p_36042_.getAmount() < i;
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(int slotID) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = getSlots().get(slotID);

        if (slot != null && slot.hasItem()) {
            ItemStack toMove = slot.getItem();
            original = toMove.copy();
            EquipmentType equipmentslot = EquipmentType.getEquipmentSlotForItem(original);
            if (slotID == 0) {
                if (!this.moveItemStackTo(toMove, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 1 && slotID < 5) {
                if (!this.moveItemStackTo(toMove, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 5 && slotID < 9) {
                if (!this.moveItemStackTo(toMove, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentslot.isArmor() && !getSlots().get(8 - equipmentslot.getIndex()).hasItem()) {
                int i = 8 - equipmentslot.getIndex();
                if (!this.moveItemStackTo(toMove, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentslot == EquipmentType.OFFHAND && !getSlots().get(45).hasItem()) {
                if (!this.moveItemStackTo(toMove, 45, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 9 && slotID < 36) {
                if (!this.moveItemStackTo(toMove, 36, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 36 && slotID < 45) {
                if (!this.moveItemStackTo(toMove, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(toMove, 9, 45, false)) {
                return ItemStack.EMPTY;
            }

            if (toMove.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            if (toMove.getAmount() == original.getAmount()) {
                return ItemStack.EMPTY;
            }
        }

        return original;
    }
}
