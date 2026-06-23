package com.frauddetection.streams;

import com.frauddetection.model.TransactionEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowedTransaction {

    private TransactionEvent currentEvent;
    private String userId;
    private int txnCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private int distinctPayees;
    private int failedTxnCount;
    private Set<String> payeesSeen = new HashSet<>();
    private long windowStartMs;
    private long windowEndMs;
}
