package dev.shadowsoffire.hostilenetworks.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.util.Constants;
import io.netty.buffer.ByteBuf;

/**
 * Packet to handle loot fabricator selection from client to server.
 */
public class LootFabSelectionMessage implements IMessage {

    public int x, y, z;
    public int selection; // -1 to clear, otherwise drop index

    public LootFabSelectionMessage() {}

    public LootFabSelectionMessage(int x, int y, int z, int selection) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.selection = selection;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.selection = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(selection);
    }

    /**
     * Handler for processing the selection message on the server.
     */
    public static class Handler implements IMessageHandler<LootFabSelectionMessage, IMessage> {

        @Override
        public IMessage onMessage(LootFabSelectionMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            TileEntity tile = player.worldObj.getTileEntity(message.x, message.y, message.z);

            if (tile instanceof LootFabTileEntity) {
                LootFabTileEntity fab = (LootFabTileEntity) tile;

                // Process selection on server
                ItemStack prediction = fab.getStackInSlot(Constants.SLOT_PREDICTION);

                if (prediction != null) {
                    String entityId = MobPredictionItem.getEntityId(prediction);

                    if (entityId != null) {
                        DataModel model = DataModelRegistry.get(entityId);

                        if (model != null) {
                            fab.setSelection(model, message.selection);
                        }
                    }
                }
            }

            return null; // No response needed
        }
    }
}
