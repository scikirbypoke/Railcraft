/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.blocks.multi;

import mods.railcraft.common.blocks.charge.Charge;
import mods.railcraft.common.blocks.charge.IChargeBlock;
import mods.railcraft.common.blocks.interfaces.ITileTanks;
import mods.railcraft.common.blocks.multi.BlockSteamTurbine.Texture;
import mods.railcraft.common.core.RailcraftConstants;
import mods.railcraft.common.fluids.FluidTools;
import mods.railcraft.common.fluids.Fluids;
import mods.railcraft.common.fluids.TankManager;
import mods.railcraft.common.fluids.tanks.FilteredTank;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.gui.GuiHandler;
import mods.railcraft.common.items.ItemTurbineRotor;
import mods.railcraft.common.items.RailcraftItems;
import mods.railcraft.common.plugins.buildcraft.triggers.INeedsMaintenance;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.plugins.ic2.IC2Plugin;
import mods.railcraft.common.plugins.ic2.IMultiEmitterDelegate;
import mods.railcraft.common.plugins.ic2.TileIC2EmitterDelegate;
import mods.railcraft.common.plugins.ic2.TileIC2MultiEmitterDelegate;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.inventory.StandaloneInventory;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.misc.Predicates;
import mods.railcraft.common.util.steam.ISteamUser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public final class TileSteamTurbine extends TileMultiBlock implements IMultiEmitterDelegate, IEnergyStorage, INeedsMaintenance, ISteamUser, ITileTanks {

    private static final int IC2_OUTPUT = 225;
    private static final int FE_OUTPUT = 900;
    private static final int STEAM_USAGE = 360;
    private static final int WATER_OUTPUT = 4;
    private static final List<MultiBlockPattern> patterns = new ArrayList<>();

    private final StandaloneInventory inv = new StandaloneInventory(1, this);
    public float output;
    private final FluidStack waterFilter = Fluids.WATER.get(2);
    protected final FilteredTank tankSteam = new FilteredTank(FluidTools.BUCKET_VOLUME * 4, this);
    protected final FilteredTank tankWater = new FilteredTank(FluidTools.BUCKET_VOLUME * 4, this);
    protected final TankManager tankManager = new TankManager();
    public static final int TANK_STEAM = 0;
    public static final int TANK_WATER = 1;
    private byte gaugeState;
    // mainGauge is a renderer field 
    public double mainGauge;
    private int energy;
    private @Nullable TileEntity emitterDelegate;
    private @Nullable IChargeBlock.ChargeBattery battery;

    static {
        char[][][] map1 = {
                {
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O', 'O'}
                },
                {
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'B', 'B', 'B', 'O'},
                        {'O', 'B', 'B', 'B', 'O'},
                        {'O', 'O', 'O', 'O', 'O'}
                },
                {
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'B', 'W', 'B', 'O'},
                        {'O', 'B', 'W', 'B', 'O'},
                        {'O', 'O', 'O', 'O', 'O'}
                },
                {
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O', 'O'}
                }
        };
        MultiBlockPattern pattern = new MultiBlockPattern(map1, Axis.X);
        patterns.add(pattern);

        char[][][] map2 = {
                {
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'}
                },
                {
                        {'O', 'O', 'O', 'O'},
                        {'O', 'B', 'B', 'O'},
                        {'O', 'B', 'B', 'O'},
                        {'O', 'B', 'B', 'O'},
                        {'O', 'O', 'O', 'O'}
                },
                {
                        {'O', 'O', 'O', 'O'},
                        {'O', 'B', 'B', 'O'},
                        {'O', 'W', 'W', 'O'},
                        {'O', 'B', 'B', 'O'},
                        {'O', 'O', 'O', 'O'}
                },
                {
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'},
                        {'O', 'O', 'O', 'O'}
                }
        };
        MultiBlockPattern otherPattern = new MultiBlockPattern(map2, Axis.Z);
        patterns.add(otherPattern);
    }

    public TileSteamTurbine() {
        super(patterns);
        tankSteam.setFilter(Fluids.STEAM);
        tankWater.setFilter(Fluids.WATER);
        tankSteam.setCanDrain(false);
        tankWater.setCanFill(false);
        tankManager.add(TANK_STEAM, tankSteam); // Steam
        tankManager.add(TANK_WATER, tankWater); // Water
    }

//    @Override
//    public ChargeHandler getChargeHandler() {
//        return chargeHandler;
//    }

    @Override
    public void update() {
        super.update();

        if (Game.isHost(world)) {
            if (isStructureValid()) {
                if (isMaster())
                    addToNet();
//                chargeHandler.tick();
            } else
                dropFromNet();

            IChargeBlock.ChargeBattery battery = getMasterBattery();
//TODO: More Cleanup ~~GC
            if (battery != null) {
                double chargeNeeded = battery.getCapacity() - battery.getCharge();
                if (chargeNeeded > 0) {
                    double draw = chargeNeeded * RailcraftConstants.EU_FE_RATIO;
                    double e = getEnergy();
                    if (e < draw)
                        draw = e;
                    removeEnergy(draw);
                    battery.addCharge(draw / RailcraftConstants.EU_FE_RATIO);
                }
            }

            if (isMaster()) {
                boolean addedEnergy = false;
                if (energy < FE_OUTPUT * 2) {
                    FluidStack steam = tankSteam.drainInternal(STEAM_USAGE, false);
//                if(steam != null) System.out.println("steam=" + steam.amount);
                    if (steam != null && steam.amount >= STEAM_USAGE) {
                        ItemStack rotor = inv.getStackInSlot(0);
                        if (RailcraftItems.TURBINE_ROTOR.isEqual(rotor) /*&& rotor.getItemDamage() < rotor.getMaxDamage() - 5*/) {
                            addedEnergy = true;
                            energy += FE_OUTPUT;
                            tankSteam.drainInternal(STEAM_USAGE, true);
                            tankWater.fillInternal(waterFilter, true);

                            inv.setInventorySlotContents(0, ((ItemTurbineRotor) rotor.getItem()).useRotor(rotor));
                        }
                    }
                }
//TODO: Cleanup and convert to Int?
                output = (float) ((output * 49D + (addedEnergy ? 100D : 0D)) / 50D);

//                System.out.println("output=" + output);
//                System.out.println("addedEnergy=" + addedEnergy);
                if (clock % 4 == 0) {
                    gaugeState = (byte) getOutput();
                    WorldPlugin.addBlockEvent(world, getPos(), getBlockType(), 1, gaugeState);
                }
            }
        }

        TankManager tMan = getTankManager();
        if (!tMan.isEmpty())
            tMan.push(tileCache, Predicates.instanceOf(TileBoilerFirebox.class), EnumFacing.HORIZONTALS, TANK_WATER, WATER_OUTPUT);
    }

    @Nullable
    IChargeBlock.ChargeBattery getMasterBattery() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock != null) {
            return mBlock.getBattery();
        }
        return null;
    }

    private IChargeBlock.ChargeBattery getBattery() {
        if (battery == null) {
            battery = Charge.util.network(world).makeBattery(pos, () -> new IChargeBlock.ChargeBattery(IC2_OUTPUT * 2, IC2_OUTPUT, 1));
        }
        return battery;
    }

    @Override
    protected void onPatternChanged() {
        super.onPatternChanged();
        if (isMaster) {
            addToNet();
        }
    }

    private void addToNet() {
        if (emitterDelegate == null)
            try {
                emitterDelegate = new TileIC2MultiEmitterDelegate(this);
                IC2Plugin.addTileToNet(emitterDelegate);
            } catch (Throwable error) {
                Game.logErrorAPI("IndustrialCraft", error);
            }
    }

    private void dropFromNet() {
        if (emitterDelegate != null) {
            IC2Plugin.removeTileFromNet(emitterDelegate);
            emitterDelegate = null;
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        dropFromNet();
        clean();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        dropFromNet();
        clean();
    }

    @Override
    protected void onMasterReset() {
        super.onMasterReset();
        dropFromNet();
        clean();
    }

    @Override
    public void onBlockRemoval() {
        super.onBlockRemoval();
        InvTools.dropInventory(inv, world, getPos());
    }

    @Override
    public boolean openGui(EntityPlayer player) {
        TileMultiBlock mBlock = getMasterBlock();
        if (mBlock != null) {
            GuiHandler.openGui(EnumGui.TURBINE, player, world, mBlock.getPos());
            return true;
        }
        return false;
    }

    public boolean hasEnergy() {
        return getEnergy() >= FE_OUTPUT;
    }

    public void removeEnergy(double amount) {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock != null) {
            mBlock.energy -= amount;
            if (mBlock.energy < 0) mBlock.energy = 0;
        }
    }

    public int getEnergy() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock == null)
            return 0;
        return mBlock.energy;
    }

    public float getOutput() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock == null)
            return 0;
        return mBlock.output;
    }

    public float getMainGauge() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock == null)
            return 0;
        return mBlock.gaugeState * 0.01F;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        inv.writeToNBT("rotor", data);
        tankManager.writeTanksToNBT(data);
        data.setInteger("energy", energy);
        data.setFloat("output", output);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        inv.readFromNBT("rotor", data);
        tankManager.readTanksFromNBT(data);
//        chargeHandler.readFromNBT(data);
        energy = data.getInteger("energy");
        output = data.getFloat("output");
    }

    @Override
    public boolean receiveClientEvent(int id, int value) {
        if (id == 1) {
            gaugeState = (byte) value;
            return true;
        }
        return super.receiveClientEvent(id, value);
    }

    @Override
    public double getOfferedEnergy() {
        if (hasEnergy())
            return IC2_OUTPUT; //Keep seperate for independent balancing
        return 0;
    }

    @Override
    public void drawEnergy(double amount) {
        removeEnergy(amount * RailcraftConstants.EU_FE_RATIO);
    }

    @Override
    public int getSourceTier() {
        return 3;
    }

    @Override
    public boolean emitsEnergyTo(TileEntity receiver, EnumFacing direction) {
        return true;
    }

    @Override
    public TileEntity getTile() {
        return this;
    }

    @Override
    public List<? extends TileEntity> getSubTiles() {
        if (!isStructureValid()) {
            return Collections.emptyList();
        }
        List<TileEntity> ret = getComponents().stream().filter(te -> te != this).map(te -> new TileIC2EmitterDelegate((TileSteamTurbine) te)).collect(Collectors.toList());
        if (emitterDelegate != null) {
            ret.add(emitterDelegate);
        }
        return ret;
    }

    public StandaloneInventory getInventory() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock != null)
            return mBlock.inv;
        return inv;
    }

    @Override
    public TankManager getTankManager() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock != null)
            return mBlock.tankManager;
        return TankManager.NIL;
    }

    //TODO: Finish Forge Energy
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (isStructureValid()) {
            if (!simulate) {
                removeEnergy(maxExtract);
            }
            return maxExtract;
        }
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return getEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return FE_OUTPUT;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return true;
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return (T) getTankManager();
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        return super.getCapability(capability, facing);
    }
    /*@Override
    public void onDisable(int duration) {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock != null) {
            mBlock.disabled = duration;
        }
    }

    @Override
    public boolean isDisabled() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock != null) {
            return mBlock.disabled <= 0;
        }
        return true;
    }*/

    @Override
    public boolean needsMaintenance() {
        TileSteamTurbine mBlock = (TileSteamTurbine) getMasterBlock();
        if (mBlock != null) {
            ItemStack rotor = mBlock.inv.getStackInSlot(0);
            if (InvTools.isEmpty(rotor))
                return true;
            if (!RailcraftItems.TURBINE_ROTOR.isEqual(rotor))
                return true;
            return rotor.getItemDamage() / (double) rotor.getMaxDamage() > 0.75f;
        }
        return false;
    }

    @Override
    public EnumGui getGui() {
        return EnumGui.TURBINE;
    }

    private void clean() {
        Charge.util.network(world).removeNode(pos);
        battery = null;
    }

    @Override
    public IBlockState getActualState(IBlockState base) {
        if (!isStructureValid()) {
            return base;
        }
        MultiBlockPattern currentPattern = getCurrentPattern();
        Axis axis = currentPattern.getAttachedData(Axis.X);
        base = base.withProperty(BlockSteamTurbine.WINDOW, getPatternMarker() == 'W')
                .withProperty(BlockSteamTurbine.LONG_AXIS, axis);
        BlockPos pos = getPatternPosition();
        final Texture texture;
        if (axis == Axis.X) {
            // x = 2, left; y = 1, bottom
            if (pos.getX() == 2) {
                if (pos.getY() == 1) {
                    texture = Texture.BOTTOM_LEFT;
                } else {
                    texture = Texture.TOP_LEFT;
                }
            } else {
                if (pos.getY() == 1) {
                    texture = Texture.BOTTOM_RIGHT;
                } else {
                    texture = Texture.TOP_RIGHT;
                }
            }
        } else {
            if (pos.getZ() == 1) {
                if (pos.getY() == 1) {
                    texture = Texture.BOTTOM_LEFT;
                } else {
                    texture = Texture.TOP_LEFT;
                }
            } else {
                if (pos.getY() == 1) {
                    texture = Texture.BOTTOM_RIGHT;
                } else {
                    texture = Texture.TOP_RIGHT;
                }
            }
        }
        return base.withProperty(BlockSteamTurbine.TEXTURE, texture);
    }
}
