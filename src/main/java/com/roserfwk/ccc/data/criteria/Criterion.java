package com.roserfwk.ccc.data.criteria;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;

public abstract class Criterion {
    protected final Identifier id;
    protected final CriterionType<?> type;
    protected boolean currentlySatisfied = false;
    protected boolean previouslySatisfied = false;

    public Criterion(Identifier id, CriterionType<?> type) {
        this.id = id;
        this.type = type;
    }

    public abstract void update(CriterionContext context);

    public boolean shouldTrigger() {
        return currentlySatisfied && !previouslySatisfied;
    }

    public void reset() {
        previouslySatisfied = false;
        currentlySatisfied = false;
    }

    protected void updateState(boolean satisfied) {
        previouslySatisfied = currentlySatisfied;
        currentlySatisfied = satisfied;
    }

    public record Raw(String type, Map<String, Object> args) {
        public Optional<Criterion> tryBuild() {
            var id = Identifier.tryParse(this.type);
            var type = CriterionRegistry.get(id);
            if (type == null) {
                return Optional.empty();
            }
            return Optional.of(type.create(id, this.args));
        }
    }

    public JsonObject serializeState() {
        var object = new JsonObject();
        object.addProperty("currently_satisfied", currentlySatisfied);
        object.addProperty("previously_satisfied", previouslySatisfied);
        return object;
    }

    public void deserializeState(JsonObject json) {
        currentlySatisfied = json.get("currently_satisfied").getAsBoolean();
        previouslySatisfied = json.get("previously_satisfied").getAsBoolean();
    }
}
