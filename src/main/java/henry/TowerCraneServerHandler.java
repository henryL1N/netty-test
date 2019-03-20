package henry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.RegExUtils;
import sun.nio.cs.ext.GBK;

import java.util.Date;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 * @author Lin Qinghua linqinghua@zhuoqin.cn
 */
@Sharable
public class TowerCraneServerHandler extends ChannelInboundHandlerAdapter {

    boolean dump = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        System.out.println(new Date());

        //转换类型
        ByteBuf byteBuf = (ByteBuf) msg;

        //打印原始数据
        if (dump) {
            StringBuilder stringBuilder = new StringBuilder();
            ByteBufUtil.appendPrettyHexDump(stringBuilder, byteBuf);
            System.out.println(stringBuilder.toString());
        }

        //帧头
        int framePrefix = byteBuf.readUnsignedShort();
//        System.out.println(String.format("Prefix: %02x", framePrefix));
        if (0x5a55 != framePrefix) {
            System.out.println("Invalid frame");
            return;
        }

        //帧长（从包括帧长本身到帧末）
        int frameLength = byteBuf.readUnsignedShortLE();
//        System.out.println("Length: " + frameLength);

        //校验和
        byteBuf.readerIndex(0);
        byte checksum = 0;
        for (int i = frameLength; i >= 0; i--) {
            int waitCount = 0;
            while (byteBuf.readerIndex() >= byteBuf.writerIndex()) {
                waitCount++;
                if (waitCount > 3) {
                    dump = true;
                    System.out.println(ByteBufUtil.prettyHexDump(byteBuf, 0, byteBuf.writerIndex()));
                    return;
                }
                System.out.println("Waiting to read");
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            byte b = byteBuf.readByte();
//            System.out.print(String.format("%02x ", b));
            checksum += b;
        }
        System.out.println();
        byte frameChecksum = byteBuf.readByte();
        if (checksum != frameChecksum) {
            System.out.println(String.format("Invalid checksum, got: %02x, expect: %02x", frameChecksum, checksum));
            return;
        }

        //帧流水号
        byteBuf.readerIndex(4);
        int frameSerial = byteBuf.readUnsignedShortLE();
//        System.out.println("Serial: " + frameSerial);

        //协议版本
        short frameProtocolVersion = byteBuf.readUnsignedByte();
//        System.out.println("Version: " + frameProtocolVersion);

        //命令
        short frameCommand = byteBuf.readUnsignedByte();
//        System.out.println("Command: %02x" + frameCommand);

        //数据载荷
        int dataLength = frameLength - 7;
        byte[] frameData = new byte[dataLength];
        byteBuf.getBytes(byteBuf.readerIndex(), frameData, 0, dataLength);
        String frameDataString = new String(frameData, new GBK());
        if (dump) {
            dump = false;
        }
        System.out.println(
                String.format(
                        "Serial: %04x, Version: %02x, Command: %02x, String: %s",
                        frameSerial,
                        frameProtocolVersion,
                        frameCommand,
                        RegExUtils.replaceAll(frameDataString, "（\\n|\\s{2,}", " ")
                )
        );
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
