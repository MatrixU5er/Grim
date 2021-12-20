package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.ItemStack;

public class ResultSlot extends Slot {

    public ResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack p_40178_) {
        return false;
    }

    @Override
    public void onTake(GrimPlayer p_150638_, ItemStack p_150639_) {
        // TODO: We should handle crafting recipe, but the server resync's here so we should be fine for now...
    }
}
