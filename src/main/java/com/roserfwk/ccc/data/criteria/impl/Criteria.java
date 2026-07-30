package com.roserfwk.ccc.data.criteria.impl;

import com.roserfwk.ccc.CustomCoyoteControl;
import com.roserfwk.ccc.data.criteria.CriterionRegistry;
import com.roserfwk.ccc.data.criteria.CriterionType;

public final class Criteria {
    private Criteria() {}

    public static final CriterionType<?> ADVANCEMENT = AdvancementCriterion::new;
    public static final CriterionType<?> BOSSBAR = BossbarCriterion::new;
    public static final CriterionType<?> DAMAGE = DamageCriterion::new;
    public static final CriterionType<?> MOB_EFFECT = MobEffectCriterion::new;
    public static final CriterionType<?> NEARBY_ENTITY = NearbyEntityCriterion::new;
    public static final CriterionType<?> PLAYER_TAG = PlayerTagCriterion::new;
    public static final CriterionType<?> SCOREBOARD = ScoreboardCriterion::new;
    public static final CriterionType<?> TEAM = TeamCriterion::new;

    public static void register() {
        CriterionRegistry.register(CustomCoyoteControl.id("advancement"), ADVANCEMENT);
        CriterionRegistry.register(CustomCoyoteControl.id("bossbar"), BOSSBAR);
        CriterionRegistry.register(CustomCoyoteControl.id("damage"), DAMAGE);
        CriterionRegistry.register(CustomCoyoteControl.id("mob_effect"), MOB_EFFECT);
        CriterionRegistry.register(CustomCoyoteControl.id("nearby_entity"), NEARBY_ENTITY);
        CriterionRegistry.register(CustomCoyoteControl.id("player_tag"), PLAYER_TAG);
        CriterionRegistry.register(CustomCoyoteControl.id("scoreboard"), SCOREBOARD);
        CriterionRegistry.register(CustomCoyoteControl.id("team"), TEAM);
    }
}
