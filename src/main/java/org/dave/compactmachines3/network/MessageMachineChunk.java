package org.dave.compactmachines3.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.dave.compactmachines3.CompactMachines3;
import org.dave.compactmachines3.utility.ChunkUtils;
import org.dave.compactmachines3.utility.Logz;
import org.dave.compactmachines3.world.tools.DimensionTools;

public class MessageMachineChunk implements IMessage, IMessageHandler<MessageMachineChunk, IMessage> {
    protected int coords;
    protected NBTTagCompound data;

    public MessageMachineChunk() {
    }

    public MessageMachineChunk(int coords) {
        this.coords = coords;
        Chunk chunk = DimensionTools.getServerMachineWorld().getChunk(new BlockPos(coords * 1024, 40, 0));
        this.data = ChunkUtils.writeChunkToNBT(chunk, DimensionTools.getServerMachineWorld(), new NBTTagCompound());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        coords = buf.readInt();
        data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(coords);

        ByteBuf tmpBuf = Unpooled.buffer();
        ByteBufUtils.writeTag(tmpBuf, data);

        if(tmpBuf.writerIndex() >= 2097152 - 8) {
            Logz.debug("Chunk data to big to send. Stripping TileEntity nbt data!");
            data.removeTag("TileEntities");

            ByteBuf tmpBufNoTiles = Unpooled.buffer();
            ByteBufUtils.writeTag(tmpBufNoTiles, data);

            if(tmpBufNoTiles.writerIndex() >= 2097152 - 8) {
                Logz.warn("Chunk data to big even without nbt data. Sending no chunk data!");
                ByteBufUtils.writeTag(buf, new NBTTagCompound());
            } else {
                buf.writeBytes(tmpBufNoTiles.readerIndex(0));
            }
            tmpBufNoTiles.release();
        } else {
            buf.writeBytes(tmpBuf.readerIndex(0));
        }

        tmpBuf.release();
    }

    @Override
    public IMessage onMessage(MessageMachineChunk message, MessageContext ctx) {
        if(!CompactMachines3.clientWorldData.isInitialized()) {
            return null;
        }

        // TODO: Clients might need to update the rendering of the machine block and its neighbors
        CompactMachines3.clientWorldData.worldClone.providerClient.loadChunkFromNBT(message.data);
        return null;
    }
}
