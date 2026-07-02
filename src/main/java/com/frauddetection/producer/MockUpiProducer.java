package com.frauddetection.producer;

import com.frauddetection.model.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockUpiProducer {

    private final TransactionProducer producer;
    private final Random random = new Random();

    private static final List<String> USER_POOL = List.of(
            "user-001", "user-002", "user-003", "user-004", "user-005",
            "user-006", "user-007", "user-008", "user-009", "user-010"
    );

    private static final List<String> PAYEE_POOL = List.of(
            "merchant@upi", "friend@okaxis", "vendor@ybl",
            "shop@paytm", "landlord@upi", "utility@upi"
    );

    // Normal traffic — every 500ms
    @Scheduled(fixedDelay = 500)
    public void sendNormalTransaction() {
        String userId = USER_POOL.get(random.nextInt(USER_POOL.size()));
        String receiverVpa = PAYEE_POOL.get(random.nextInt(PAYEE_POOL.size()));
        BigDecimal amount = BigDecimal.valueOf(50 + random.nextInt(4951)); // 50–5000

        TransactionEvent event = buildEvent(userId, amount, receiverVpa, false,
                TransactionEvent.TxnStatus.SUCCESS, TransactionEvent.PaymentMode.P2P);

        producer.send(event);
    }

    // Velocity fraud — 6 rapid txns from user-003 every 30s
    @Scheduled(fixedDelay = 30000)
    public void injectVelocityFraud() {
        log.warn("[MOCK] Injecting velocity fraud for user-003");
        for (int i = 0; i < 6; i++) {
            String receiverVpa = "target" + i + "@upi";
            TransactionEvent event = buildEvent("user-003",
                    BigDecimal.valueOf(500 + random.nextInt(500)),
                    receiverVpa, false,
                    TransactionEvent.TxnStatus.SUCCESS, TransactionEvent.PaymentMode.P2P);
            producer.send(event);
        }
    }

    // Large amount + new payee — user-007 every 60s
    @Scheduled(fixedDelay = 60000)
    public void injectLargeAmountFraud() {
        log.warn("[MOCK] Injecting large amount + new payee fraud for user-007");
        TransactionEvent event = buildEvent("user-007",
                BigDecimal.valueOf(75000),
                "unknown-payee-" + System.currentTimeMillis() + "@upi",
                false,
                TransactionEvent.TxnStatus.SUCCESS, TransactionEvent.PaymentMode.P2M);
        producer.send(event);
    }

    // New device + high amount — user-005 every 90s
    @Scheduled(fixedDelay = 90000)
    public void injectNewDeviceFraud() {
        log.warn("[MOCK] Injecting new device fraud for user-005");
        TransactionEvent event = buildEvent("user-005",
                BigDecimal.valueOf(15000),
                "merchant@upi",
                true,
                TransactionEvent.TxnStatus.SUCCESS, TransactionEvent.PaymentMode.P2M);
        producer.send(event);
    }

    private TransactionEvent buildEvent(String userId, BigDecimal amount, String receiverVpa,
                                        boolean isNewDevice, TransactionEvent.TxnStatus status,
                                        TransactionEvent.PaymentMode paymentMode) {
        String txnId = UUID.randomUUID().toString();
        return TransactionEvent.builder()
                .txnId(txnId)
                .userId(userId)
                .sessionId("session-" + userId + "-" + System.currentTimeMillis())
                .amount(amount)
                .currency("INR")
                .status(status)
                .senderVpa(userId.replace("-", "") + "@okaxis")
                .receiverVpa(receiverVpa)
                .paymentMode(paymentMode)
                .deviceId(hashDeviceId(isNewDevice ? "new-device-" + UUID.randomUUID() : "device-" + userId))
                .ipAddress("192.168.1." + (random.nextInt(254) + 1))
                .isNewDevice(isNewDevice)
                .timestamp(System.currentTimeMillis())
                .channel(TransactionEvent.Channel.UPI)
                .idempotencyKey(txnId + ":1")
                .build();
    }

    private String hashDeviceId(String rawDeviceId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawDeviceId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
