package com.example.odp.message;

import com.example.odp.constants.OdpConstants;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class Heartbeat {
    
    private String compId;
    private long msgSeqNum;
    
    public OdpHeader buildHeader() {
        return OdpHeader.builder()
                .length(OdpConstants.HEADER_SIZE)
                .messageId(OdpConstants.MSG_ID_HEARTBEAT)
                .msgSeqNum(msgSeqNum)
                .compId(compId)
                .messageFlags(0)
                .fieldsPresenceMap(new byte[OdpConstants.FIELDS_PRESENCE_MAP_SIZE])
                .build();
    }
}