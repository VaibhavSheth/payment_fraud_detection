package com.frauddetection.api;

import com.frauddetection.model.AuditLog;
import com.frauddetection.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudReportController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/alerts")
    public Page<AuditLog> getAlerts(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String rule,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size);
        List<AuditLog> results;

        if (userId != null) {
            results = auditLogRepository.findByUserId(userId);
        } else if (rule != null) {
            results = auditLogRepository.findByDecision("BLOCK").stream()
                    .filter(a -> rule.equals(a.getTriggeredRule()))
                    .toList();
        } else if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.atTime(23, 59, 59);
            results = auditLogRepository.findByDecidedAtBetween(from, to);
        } else {
            results = auditLogRepository.findAll();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        List<AuditLog> pageContent = start >= results.size() ? List.of() : results.subList(start, end);

        return new PageImpl<>(pageContent, pageable, results.size());
    }

    @GetMapping("/alerts/{txnId}")
    public ResponseEntity<AuditLog> getAlert(@PathVariable String txnId) {
        return auditLogRepository.findByTxnId(txnId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/alerts/{txnId}/override")
    public ResponseEntity<Map<String, Object>> overrideAlert(
            @PathVariable String txnId,
            @RequestBody Map<String, String> body) {

        return auditLogRepository.findByTxnId(txnId)
                .map(log -> {
                    log.setOverridden(true);
                    log.setOverrideReason(body.get("overrideReason"));
                    auditLogRepository.save(log);
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "txnId", txnId,
                            "overridden", true,
                            "overrideReason", body.get("overrideReason"),
                            "overriddenAt", LocalDateTime.now().toString()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats/today")
    public Map<String, Object> getTodayStats() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        long total = auditLogRepository.countByDecisionAndDateRange("ALLOW", from, to)
                + auditLogRepository.countByDecisionAndDateRange("BLOCK", from, to);
        long flagged = auditLogRepository.countByDecisionAndDateRange("BLOCK", from, to);
        long allowed = auditLogRepository.countByDecisionAndDateRange("ALLOW", from, to);
        String flagRate = total > 0 ? String.format("%.2f%%", (flagged * 100.0 / total)) : "0.00%";

        return Map.of(
                "date", LocalDate.now().toString(),
                "total", total,
                "flagged", flagged,
                "allowed", allowed,
                "flagRate", flagRate
        );
    }

    @GetMapping("/stats/rules")
    public List<Map<String, Object>> getRuleStats() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        List<String> ruleNames = List.of(
                "VELOCITY_ABUSE", "LARGE_AMOUNT_NEW_PAYEE",
                "NEW_DEVICE_HIGH_AMOUNT", "FAILED_ATTEMPTS"
        );

        return ruleNames.stream().map(rule -> {
            long last7Days = auditLogRepository.findByDecidedAtBetween(sevenDaysAgo, now)
                    .stream().filter(a -> rule.equals(a.getTriggeredRule())).count();
            long last24Hours = auditLogRepository.findByDecidedAtBetween(oneDayAgo, now)
                    .stream().filter(a -> rule.equals(a.getTriggeredRule())).count();
            return Map.<String, Object>of(
                    "ruleName", rule,
                    "last7Days", last7Days,
                    "last24Hours", last24Hours
            );
        }).toList();
    }
}
