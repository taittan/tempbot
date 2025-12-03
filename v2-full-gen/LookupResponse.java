package com.example.odp.message;

import com.example.odp.constants.OdpConstants;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupResponse {
    
    // Header
    private OdpHeader header;
    
    // Body 字段
    private int lookupStatus;
    private int lookupRejectReason;
    private String ipAddress1;
    private int portNumber1;
    private String ipAddress2;
    private int portNumber2;
    
    public boolean isAccepted() {
        return lookupStatus == OdpConstants.LOOKUP_STATUS_ACCEPTED;
    }
    
    public boolean isRejected() {
        return lookupStatus == OdpConstants.LOOKUP_STATUS_REJECTED;
    }
    
    public String getRejectReasonDescription() {
        return switch (lookupRejectReason) {
            case OdpConstants.LOOKUP_REJECT_INVALID_COMPID -> "Invalid CompID or invalid IP address";
            case OdpConstants.LOOKUP_REJECT_BLOCKED -> "Client (CompID) is blocked";
            default -> "Unknown reason: " + lookupRejectReason;
        };
    }
    
    @Override
    public String toString() {
        if (isAccepted()) {
            return String.format("LookupResponse[ACCEPTED, Primary=%s:%d, Secondary=%s:%d]",
                    ipAddress1, portNumber1, ipAddress2, portNumber2);
        } else {
            return String.format("LookupResponse[REJECTED, Reason=%d (%s)]",
                    lookupRejectReason, getRejectReasonDescription());
        }
    }
}