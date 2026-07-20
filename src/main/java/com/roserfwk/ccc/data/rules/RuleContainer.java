package com.roserfwk.ccc.data.rules;

import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;

import java.util.Collection;

public final class RuleContainer {
    private Rule rule;
    private final String id;
    private long lastTriggerTime = -1;
    private boolean triggered = false;

    public RuleContainer(Rule rule, String id) {
        this.rule = rule;
        this.id = id;
    }

    public RuleContainer(Rule rule, String id, long lastTriggerTime, boolean triggered) {
        this(rule, id);
        this.lastTriggerTime = lastTriggerTime;
        this.triggered = triggered;
    }

    public void update(CriterionContext context) {
        if (triggered) return;

        if (!canTrigger()) return;

        if (rule.update(context)) {
            triggered = true;
            lastTriggerTime = System.currentTimeMillis();
        }

        if (getRemainingCooldown() <= 0) {
            triggered = false;
        }
    }

    private boolean canTrigger() {
        if (rule.cooldown() <= 0) {
            return true;
        }

        if (lastTriggerTime < 0) {
            return true;
        }

        var currentTime = System.currentTimeMillis();
        var elapsed = currentTime - lastTriggerTime;
        return elapsed >= rule.cooldown();
    }

    public long getRemainingCooldown() {
        if (rule.cooldown() <= 0) {
            return 0;
        }

        if (lastTriggerTime < 0) {
            return 0;
        }

        var currentTime = System.currentTimeMillis();
        var elapsed = currentTime - lastTriggerTime;
        var remaining = rule.cooldown() - elapsed;

        return Math.max(0, remaining);
    }

    void refreshRule(Rule rule) {
        this.rule = rule;
    }

    Collection<Criterion> criteria() {
        return rule.criteria();
    }

    Rule rule() {
        return rule;
    }

    String id() {
        return id;
    }

    long lastTriggerTime() {
        return lastTriggerTime;
    }

    boolean triggered() {
        return triggered;
    }
}
