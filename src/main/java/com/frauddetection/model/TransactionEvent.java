package com.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    @Builder.Default
    private String txnId = UUID.randomUUID().toString();

    private String userId;
    private String sessionId;

    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private TxnStatus status = TxnStatus.INITIATED;

    private String senderVpa;
    private String receiverVpa;

    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.P2P;

    private String deviceId;
    private String ipAddress;

    @JsonProperty("isNewDevice")
    private boolean isNewDevice;

    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    @Builder.Default
    private Channel channel = Channel.UPI;

    @Builder.Default
    private String idempotencyKey = UUID.randomUUID().toString() + ":1";

    public enum TxnStatus {
        INITIATED, SUCCESS, FAILED, REVERSED
    }

    public enum PaymentMode {
        P2P, P2M, COLLECT
    }

    public enum Channel {
        UPI, CARD, NETBANKING, WALLET
    }
}
