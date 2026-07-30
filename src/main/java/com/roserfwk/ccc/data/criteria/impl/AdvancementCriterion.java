package com.roserfwk.ccc.data.criteria.impl;

import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class AdvancementCriterion extends Criterion {
    private final String advancementId;

    public AdvancementCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.ADVANCEMENT);
        this.advancementId = (String) args.get("advancement");
    }

    @Override
    public void update(CriterionContext context) {
        var advancementIdentifier = Identifier.tryParse(advancementId);
        if (advancementIdentifier == null) {
            updateState(false);
            return;
        }

        var player = context.getTargetPlayer();
        var advancement = context.getServer().getAdvancements()
                .get(advancementIdentifier);

        if (advancement == null) {
            updateState(false);
            return;
        }

        var tracker = player.getAdvancements();
        var progress = tracker.getOrStartProgress(advancement);

        updateState(progress.isDone());
    }
}
