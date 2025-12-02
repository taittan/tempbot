public class Main {
    public static void main(String[] args) {
        // 构造 Lookup Request
        String compId = "CLIENT001";
        ByteBuf request = LookupRequestBuilder.buildLookupRequest(compId);
        
        // 打印二进制内容（调试用）
        System.out.println("Lookup Request bytes: " + request.readableBytes());
        printHex(request);
        
        // 通过 Netty 发送 request...
    }
    
    private static void printHex(ByteBuf buffer) {
        StringBuilder sb = new StringBuilder();
        int readerIndex = buffer.readerIndex();
        for (int i = 0; i < buffer.readableBytes(); i++) {
            sb.append(String.format("%02X ", buffer.getByte(readerIndex + i)));
        }
        System.out.println(sb.toString());
    }
}