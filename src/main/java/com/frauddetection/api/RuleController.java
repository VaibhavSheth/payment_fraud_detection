package com.frauddetection.api;

import com.frauddetection.api.dto.CreateRuleRequest;
import com.frauddetection.api.dto.UpdateRuleRequest;
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
    public ResponseEntity<FraudRule> createRule(@Valid @RequestBody CreateRuleRequest request) {
        if (ruleRepository.findByRuleName(request.getRuleName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        FraudRule rule = new FraudRule();
        rule.setRuleName(request.getRuleName());
        rule.setDescription(request.getDescription());
        rule.setRuleType(request.getRuleType());
        rule.setThreshold(request.getThreshold());
        rule.setWindowSeconds(request.getWindowSeconds());
        rule.setPriority(request.getPriority());
        rule.setActive(true);

        FraudRule saved = ruleRepository.save(rule);
        ruleLoaderService.reloadRules();
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FraudRule> updateRule(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateRuleRequest request) {
        return ruleRepository.findById(id)
                .map(rule -> {
                    if (request.getThreshold() != null) rule.setThreshold(request.getThreshold());
                    if (request.getDescription() != null) rule.setDescription(request.getDescription());
                    if (request.getPriority() != null) rule.setPriority(request.getPriority());
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
