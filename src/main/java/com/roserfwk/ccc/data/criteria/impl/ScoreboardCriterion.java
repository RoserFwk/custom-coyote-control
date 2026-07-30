package com.roserfwk.ccc.data.criteria.impl;

import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class ScoreboardCriterion extends Criterion {
    private final String objectiveName;
    private final ComparisonType comparison;
    private final int threshold;
    private final Integer upperBound;

    public ScoreboardCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.SCOREBOARD);
        this.objectiveName = (String) args.get("objective");
        this.comparison = ComparisonType.valueOf(((String) args.get("comparison")).toUpperCase());
        this.threshold = ((Number) args.get("threshold")).intValue();
        this.upperBound = args.containsKey("upper_bound") ?
                ((Number) args.get("upper_bound")).intValue() : null;
    }

    @Override
    public void update(CriterionContext context) {
        var player = context.getTargetPlayer();
        var scoreboard = context.getServer().getScoreboard();

        var objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            updateState(false);
            return;
        }

        var score = scoreboard.getOrCreatePlayerScore(player, objective);
        var value = score.get();

        boolean satisfied;
        if (upperBound != null) {
            satisfied = value >= threshold && value <= upperBound;
        } else {
            satisfied = switch (comparison) {
                case EQUAL -> value == threshold;
                case GREATER -> value > threshold;
                case LESS -> value < threshold;
                case GREATER_EQUAL -> value >= threshold;
                case LESS_EQUAL -> value <= threshold;
            };
        }

        updateState(satisfied);
    }

    public enum ComparisonType {
        EQUAL, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL
    }
}
