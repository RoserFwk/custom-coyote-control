package com.roserfwk.ccc.data.criteria.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

public class NearbyEntityCriterion extends Criterion {
    private final Holder<EntityType<?>> entityType;
    private final EntitySelector selector;
    private CommandSourceStack src = null;

    public NearbyEntityCriterion(Identifier id, Map<String, Object> args) {
        super(id, Criteria.NEARBY_ENTITY);

        var entityTypeId = Identifier.tryParse((String) args.get("entity_type"));
        if (entityTypeId == null) {
            throw new IllegalArgumentException(String.format("Invalid entity type: %s", args.get("entity_type")));
        }

        var entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (entityType.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid entity type: %s", args.get("entity_type")));
        }
        this.entityType = entityType.get();

        var selector = (String) args.get("selector");
        if (selector == null) {
            throw new IllegalArgumentException("Selector cannot be null");
        }

        var arg = EntityArgument.entities();
        try {
            this.selector = arg.parse(new StringReader(selector));
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException(String.format("Invalid selector: %s", args.get("selector")));
        }
    }

    @Override
    public void update(CriterionContext context) {
        if (entityType == null) {
            updateState(false);
            return;
        }

        if (src == null) {
            var player = context.getTargetPlayer();
            src = player.createCommandSourceStack();
        }

        try {
            var nearbyEntities = selector.findEntities(src);
            updateState(!nearbyEntities.isEmpty());
        } catch (CommandSyntaxException e) {
            updateState(false);
        }
    }
}
