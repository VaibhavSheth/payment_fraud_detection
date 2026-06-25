package com.frauddetection.rules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<FraudRule, Long> {

    List<FraudRule> findByIsActiveTrueOrderByPriorityAsc();

    Optional<FraudRule> findByRuleName(String ruleName);
}
