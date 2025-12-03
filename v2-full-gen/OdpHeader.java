package com.example.odp.message;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdpHeader {
    
    private int length;
    private int messageId;
    private long msgSeqNum;
    private String compId;
    private int messageFlags;
    private byte[] fieldsPresenceMap;
    
    public boolean isPossDup() {
        return (messageFlags & 0x01) != 0;
    }
    
    public boolean isPossResend() {
        return (messageFlags & 0x02) != 0;
    }
    
    @Override
    public String toString() {
        return String.format("Header[len=%d, msgId=%d, seqNum=%d, compId=%s, flags=0x%02X]",
                length, messageId, msgSeqNum, compId, messageFlags);
    }
}