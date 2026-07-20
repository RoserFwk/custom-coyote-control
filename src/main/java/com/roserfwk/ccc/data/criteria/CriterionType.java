package com.roserfwk.ccc.data.criteria;

import net.minecraft.resources.Identifier;

import java.util.Map;

@FunctionalInterface
public interface CriterionType<T extends Criterion> {
    T create(Identifier id, Map<String, Object> args);
}
