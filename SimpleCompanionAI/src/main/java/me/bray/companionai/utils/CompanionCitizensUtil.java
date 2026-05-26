package me.bray.companionai.utils;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

import java.util.UUID;

public class CompanionCitizensUtil {

    public static void setOwner(NPC npc, UUID ownerUuid) {
        if (npc == null || ownerUuid == null) {
            return;
        }

        npc.getOrAddTrait(Owner.class).setOwner(ownerUuid);
    }
}