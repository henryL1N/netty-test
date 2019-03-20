package henry;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import sun.nio.cs.ext.GBK;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * @author Lin Qinghua linqinghua@zhuoqin.cn
 */
@Sharable
public class TowerCraneServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        int framePrefix = byteBuf.readUnsignedShort();
        System.out.println(String.format("Prefix: %x", framePrefix));
        if (0x5a55 != framePrefix) {
            System.out.println("Invalid frame");
        }
        int frameLength = byteBuf.readUnsignedShortLE();
        System.out.println("Length: " + frameLength);
        byteBuf.readerIndex(2);
        long checksum = 0L;
        while (frameLength > 1) {
            checksum += byteBuf.readUnsignedByte();
            frameLength--;
        }
        short checksumByte = (short) ((int) checksum & 0xFF);
        short frameChecksum = byteBuf.readUnsignedByte();
        if (checksumByte != frameChecksum) {
            System.out.println("Invalid checksum, got: \"" + frameChecksum + "\", expect: \"" + checksumByte + "\"");
        }
        byteBuf.readerIndex(4);
        int frameSerial = byteBuf.readUnsignedShortLE();
        System.out.println("Serial: " + frameSerial);
        short frameProtocolVersion = byteBuf.readUnsignedByte();
        System.out.println("Version: " + frameProtocolVersion);
        short frameCommand = byteBuf.readUnsignedByte();
        System.out.println("Command: " + frameCommand);
        int dataLength = frameLength - 7;
        byte[] frameData = new byte[dataLength];
        byteBuf.getBytes(byteBuf.readerIndex(), frameData, 0, dataLength);
        String frameDataString = new String(frameData, new GBK());
        System.out.println("String: " + frameDataString);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
