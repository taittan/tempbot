package com.example.odp.message;

import com.example.odp.constants.OdpConstants;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class LookupRequest {
    
    private String compId;
    
    public OdpHeader buildHeader() {
        return OdpHeader.builder()
                .length(OdpConstants.HEADER_SIZE)
                .messageId(OdpConstants.MSG_ID_LOOKUP_REQUEST)
                .msgSeqNum(1L)  // Lookup Request 的 MsgSeqNum 必须为 1
                .compId(compId)
                .messageFlags(0)
                .fieldsPresenceMap(new byte[OdpConstants.FIELDS_PRESENCE_MAP_SIZE])
                .build();
    }
}