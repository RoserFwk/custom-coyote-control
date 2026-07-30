package com.roserfwk.ccc.data.criteria.impl;

import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class BossbarCriterion extends Criterion {
    private final Identifier bossbarId;

    public BossbarCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.BOSSBAR);
        this.bossbarId = Identifier.tryParse((String) args.get("bossbar_id"));
    }

    @Override
    public void update(CriterionContext context) {
        if (bossbarId == null) {
            updateState(false);
            return;
        }

        var player = context.getTargetPlayer();
        var bossbar = context.getServer().getCustomBossEvents().get(bossbarId);

        if (bossbar == null) {
            updateState(false);
            return;
        }

        updateState(bossbar.getPlayers().contains(player));
    }
}
