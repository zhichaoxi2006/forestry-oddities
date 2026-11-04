package com.forestrytweaks.content;

import com.forestrytweaks.content.alveary.AlvearyFrameHousingBlockEntity;
import com.forestrytweaks.content.alveary.AlvearyStimulatorBlockEntity;
import com.forestrytweaks.content.alveary.AlvearyTransmissionBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.world.item.ItemStack;

/**
 * Capability providers.
 *
 * <p>The soft light frame exposes its internal buffer as a regular NeoForge item energy storage, so
 * FE chargers (charging stations, machine item slots, energy pipes that fill items, ...) can fill it
 * without this mod knowing about them. The frame is not externally drainable: it spends the energy
 * itself, per work cycle.</p>
 *
 * <p>The two powered alveary parts expose receive-only block buffers for the same reason: any FE
 * source can charge a stimulator, and a transmission hands that energy to the rest of the alveary.
 * Neither part can be drained through its capability.</p>
 *
 * <p>The capability handlers are cached per stack / block entity by NeoForge, so the item storage
 * below must read and write the stack's component on every call instead of holding the value in a
 * field.</p>
 */
public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, context) -> new SoftLightFrameEnergy(stack),
            ModItems.SOFT_LIGHT_FRAME.get());

        // The alveary frame housing's one frame slot, so pipes and hoppers can insert and pull frames.
        AlvearyFrameHousingBlockEntity.registerCapabilities(event);
        // The FE buffers of the two powered parts. Neither has an item handler: the stimulator's
        // board sits in a Forestry socket, which automation cannot reach.

        AlvearyStimulatorBlockEntity.registerCapabilities(event);
        AlvearyTransmissionBlockEntity.registerCapabilities(event);
    }

    private record SoftLightFrameEnergy(ItemStack stack) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) {
                return 0;
            }

            int stored = SoftLightFrameItem.getEnergy(this.stack);
            int received = Math.min(SoftLightFrameItem.CAPACITY - stored, toReceive);
            if (received <= 0) {
                return 0;
            }

            if (!simulate) {
                SoftLightFrameItem.setEnergy(this.stack, stored + received);
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return SoftLightFrameItem.getEnergy(this.stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return SoftLightFrameItem.CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
