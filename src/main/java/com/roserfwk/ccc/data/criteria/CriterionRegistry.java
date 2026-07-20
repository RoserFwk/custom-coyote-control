package com.roserfwk.ccc.data.criteria;

import com.roserfwk.ccc.CustomCoyoteControl;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;

public class CriterionRegistry {
    public static final ResourceKey<Registry<CriterionType<?>>> CRITERION_TYPE_KEY =
            ResourceKey.createRegistryKey(CustomCoyoteControl.id("criterion_type"));
    public static final Registry<CriterionType<?>> REGISTRY =
            FabricRegistryBuilder.create(CRITERION_TYPE_KEY)
                    .attribute(RegistryAttribute.OPTIONAL)
                    .buildAndRegister();

    private static final Map<Identifier, CriterionType<?>> TYPES = new HashMap<>();

    public static <T extends Criterion> CriterionType<T> register(
            Identifier id,
            CriterionType<T> type
    ) {
        Registry.register(REGISTRY, id, type);
        TYPES.put(id, type);
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Criterion> CriterionType<T> get(Identifier id) {
        return (CriterionType<T>) TYPES.get(id);
    }

    public static boolean contains(Identifier id) {
        return TYPES.containsKey(id);
    }
}
