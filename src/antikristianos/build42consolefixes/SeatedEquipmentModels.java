package antikristianos.build42consolefixes;

import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.network.GameServer;

/** Model admission only. Native equipment, inventory and packet state are never changed. */
public final class SeatedEquipmentModels {
    private SeatedEquipmentModels() { }

    public static boolean seated(IsoGameCharacter character) {
        return !GameServer.server && character != null && character.getVehicle() != null;
    }

    public static boolean handProp(IsoGameCharacter character, String bone) {
        return seated(character) && ("Bip01_Prop1".equals(bone) || "Bip01_Prop2".equals(bone));
    }

    public static boolean attachedItem(ModelInstance parent, String model, String attachment) {
        if (parent == null || model == null || attachment == null) return false;
        IsoGameCharacter character = parent.character;
        if (!seated(character)) return false;
        var attached = character.getAttachedItems();
        if (attached == null || attached.getGroup() == null) return false;
        for (int i = 0; i < attached.size(); i++) {
            var entry = attached.get(i);
            var item = entry.getItem();
            if (item == null) continue;
            var location = attached.getGroup().getLocation(entry.getLocation());
            if (location != null && attachment.equals(location.getAttachmentName())
                    && model.equals(item.getStaticModelException())) return true;
        }
        return false;
    }

    public static void vehicleChanged(IsoGameCharacter character, boolean wasSeated) {
        if (!GameServer.server && character != null && wasSeated != (character.getVehicle() != null))
            character.resetEquippedHandsModels();
    }
}
