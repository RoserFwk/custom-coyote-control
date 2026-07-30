package com.roserfwk.ccc.data.criteria.impl;

import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class TeamCriterion extends Criterion {
    private final String teamName;

    public TeamCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.TEAM);
        this.teamName = (String) args.get("team");
    }

    @Override
    public void update(CriterionContext context) {
        var team = context.getServer().getScoreboard().getPlayerTeam(teamName);
        if (team == null) {
            updateState(false);
            return;
        }

        updateState(team.getPlayers().contains(
                context.getTargetPlayer().getName().getString()));
    }
}
