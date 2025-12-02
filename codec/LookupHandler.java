import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LookupHandler extends ChannelInboundHandlerAdapter {

    private final String compId;
    
    public LookupHandler(String compId) {
        this.compId = compId;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 连接建立后，发送 Lookup Request
        ByteBuf request = LookupRequestBuilder.buildLookupRequest(compId);
        ctx.writeAndFlush(request);
        System.out.println("Sent Lookup Request for CompId: " + compId);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buffer = (ByteBuf) msg;
        
        // 先 peek Message ID 判断消息类型
        int messageId = buffer.getUnsignedShort(2); // offset 2 是 Message ID 位置
        
        if (messageId == OcgConstants.MSG_ID_LOOKUP_RESPONSE) {
            LookupResponseParser.LookupResponse response = LookupResponseParser.parse(buffer);
            System.out.println("Received: " + response);
            
            if (response.isAccepted()) {
                // 使用 response.ipAddress1, response.portNumber1 建立新连接
                // 或使用 response.ipAddress2, response.portNumber2 作为备用
                System.out.println("Primary: " + response.ipAddress1 + ":" + response.portNumber1);
                System.out.println("Secondary: " + response.ipAddress2 + ":" + response.portNumber2);
            } else {
                System.out.println("Lookup rejected, reason: " + response.lookupRejectReason);
            }
        }
        
        buffer.release();
    }
}