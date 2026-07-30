package com.roserfwk.ccc.data.criteria.impl;

import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class PlayerTagCriterion extends Criterion {
    private final String tag;

    public PlayerTagCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.PLAYER_TAG);
        this.tag = (String) args.get("tag");
    }

    @Override
    public void update(CriterionContext context) {
        updateState(context.getTargetPlayer().entityTags().contains(tag));
    }
}
