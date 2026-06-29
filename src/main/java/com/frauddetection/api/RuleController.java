package com.frauddetection.api;

import com.frauddetection.rules.FraudRule;
import com.frauddetection.rules.RuleRepository;
import com.frauddetection.service.RuleLoaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleRepository ruleRepository;
    private final RuleLoaderService ruleLoaderService;

    @GetMapping
    public List<FraudRule> getAllActiveRules() {
        return ruleRepository.findByIsActiveTrueOrderByPriorityAsc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraudRule> getRuleById(@PathVariable Long id) {
        return ruleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FraudRule> createRule(@Valid @RequestBody FraudRule rule) {
        if (ruleRepository.findByRuleName(rule.getRuleName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        FraudRule saved = ruleRepository.save(rule);
        ruleLoaderService.reloadRules();
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FraudRule> updateRule(@PathVariable Long id,
                                                 @RequestBody FraudRule updates) {
        return ruleRepository.findById(id)
                .map(rule -> {
                    if (updates.getThreshold() != null) rule.setThreshold(updates.getThreshold());
                    if (updates.getDescription() != null) rule.setDescription(updates.getDescription());
                    if (updates.getPriority() > 0) rule.setPriority(updates.getPriority());
                    FraudRule saved = ruleRepository.save(rule);
                    ruleLoaderService.reloadRules();
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateRule(@PathVariable Long id) {
        return ruleRepository.findById(id)
                .map(rule -> {
                    rule.setActive(false);
                    ruleRepository.save(rule);
                    ruleLoaderService.reloadRules();
                    return ResponseEntity.ok(Map.of(
                            "message", "Rule " + rule.getRuleName() + " deactivated",
                            "effectiveIn", "immediate"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, String>> activateRule(@PathVariable Long id) {
        return ruleRepository.findById(id)
                .map(rule -> {
                    rule.setActive(true);
                    ruleRepository.save(rule);
                    ruleLoaderService.reloadRules();
                    return ResponseEntity.ok(Map.of(
                            "message", "Rule " + rule.getRuleName() + " activated",
                            "effectiveIn", "immediate"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
