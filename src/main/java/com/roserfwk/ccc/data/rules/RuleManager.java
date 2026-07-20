package com.roserfwk.ccc.data.rules;

import com.google.gson.*;
import com.roserfwk.ccc.CustomCoyoteControl;
import com.roserfwk.ccc.data.criteria.Criterion;
import com.roserfwk.ccc.data.criteria.CriterionContext;
import com.roserfwk.ccc.utils.CCCMinecraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Rule> rules = new ConcurrentHashMap<>();
    private final Map<UUID, Set<RuleContainer>> enabledRules = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> ruleSets = new ConcurrentHashMap<>();

    public static RuleManager getInstance(MinecraftServer server) {
        return ((CCCMinecraftServer) server).ccc$getRuleManager();
    }

    public void updateRules(CriterionContext context) {
        for (var rules : enabledRules.values()) {
            for (var rule : rules) {
                rule.update(context);
            }
        }
    }

    public Collection<String> getRuleIds() {
        return rules.keySet();
    }

    public Collection<String> getRuleSetIds() {
        return ruleSets.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends Criterion> void handleCriterion(Class<T> clazz, Consumer<T> consumer) {
        for (var containers : enabledRules.values()) {
            for (var rule : containers) {
                for (var criterion : rule.criteria()) {
                    if (clazz.isAssignableFrom(criterion.getClass())) {
                        consumer.accept((T) criterion);
                    }
                }
            }
        }
    }

    public void enableRule(UUID uuid, String ruleId) {
        Optional.ofNullable(rules.get(ruleId)).ifPresentOrElse(rule ->
                Optional.ofNullable(enabledRules.get(uuid)).ifPresentOrElse(rules ->
                        rules.add(new RuleContainer(rule.clone(), ruleId)),
                        () -> {
                    enabledRules.put(uuid, new HashSet<>());
                    enabledRules.get(uuid).add(new RuleContainer(rule.clone(), ruleId));
                        }),
                () -> CustomCoyoteControl.LOGGER.warn("Could not find rule with id {}", ruleId)
        );
    }

    public void disableRule(UUID uuid, String ruleId) {
        Optional.ofNullable(enabledRules.get(uuid)).ifPresent(rules ->
                rules.removeIf(rule -> rule.id().equals(ruleId)));
    }

    public void enableRuleSet(UUID uuid, String ruleSetId) {
        //noinspection LoggingSimilarMessage
        Optional.ofNullable(ruleSets.get(ruleSetId)).ifPresentOrElse(
                rules -> rules.forEach(rule -> enableRule(uuid, rule)),
                () -> CustomCoyoteControl.LOGGER.warn("Could not find rule set with id {}", ruleSetId)
        );
    }

    public void disableRuleSet(UUID uuid, String ruleSetId) {
        //noinspection LoggingSimilarMessage
        Optional.ofNullable(ruleSets.get(ruleSetId)).ifPresentOrElse(
                rules -> rules.forEach(rule -> disableRule(uuid, rule)),
                () -> CustomCoyoteControl.LOGGER.warn("Could not find rule set with id {}", ruleSetId)
        );
    }

    public void reload(MinecraftServer server) {
        rules.clear();
        ruleSets.clear();

        var rulesDir = getRulesDir(server);
        if (!Files.isDirectory(rulesDir)) {
            CustomCoyoteControl.LOGGER.error("Rules folder is not a directory!");
            try {
                Files.deleteIfExists(rulesDir);
                Files.createDirectories(rulesDir);
            } catch (IOException e) {
                CustomCoyoteControl.LOGGER.error("Failed to create rules folder!", e);
            }
            return;
        }

        var ruleSetsDir = getRuleSetsDir(server);
        if (!Files.isDirectory(ruleSetsDir)) {
            CustomCoyoteControl.LOGGER.warn("Rule sets folder is not a directory!");
            try {
                Files.deleteIfExists(ruleSetsDir);
                Files.createDirectories(ruleSetsDir);
            } catch (IOException e) {
                CustomCoyoteControl.LOGGER.error("Failed to create rulesets folder!", e);
            }
        }

        try {
            Files.walkFileTree(rulesDir, new SimpleFileVisitor<>() {
                @Override
                public @NonNull FileVisitResult visitFile(
                        @NotNull Path file,
                        @NonNull BasicFileAttributes attrs
                ) throws IOException {
                    if (Files.isRegularFile(file)
                            && file.getFileName().toString().endsWith(".json")) {
                        try (var is = Files.newInputStream(file);
                             var reader = new InputStreamReader(is);) {
                            var rule = GSON.fromJson(reader, Rule.class);

                            if (rule != null) {
                                rules.put(removeExtension(file.toString()), rule);
                            }
                        }
                    }
                    return super.visitFile(file, attrs);
                }
            });

            if (Files.isDirectory(ruleSetsDir)) {
                Files.walkFileTree(ruleSetsDir, new SimpleFileVisitor<>() {
                    @Override
                    public @NonNull FileVisitResult visitFile(
                            @NotNull Path file,
                            @NonNull BasicFileAttributes attrs
                    ) throws IOException {
                        if (Files.isRegularFile(file)
                                && file.getFileName().toString().endsWith(".json")) {
                            try (var is = Files.newInputStream(file);
                                 var reader = new InputStreamReader(is);) {
                                var ruleSetElem = JsonParser.parseReader(reader);

                                if (ruleSetElem != null && ruleSetElem.isJsonArray()) {
                                    var ruleIds = new HashSet<String>();
                                    for (var entry : ruleSetElem.getAsJsonArray()) {
                                        ruleIds.add(entry.getAsString());
                                    }

                                    ruleSets.put(removeExtension(file.toString()), ruleIds);
                                }
                            }
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            }
        } catch (IOException e) {
            CustomCoyoteControl.LOGGER.error("Error while loading rules folder!", e);
            rules.clear();
            ruleSets.clear();
        }

        enabledRules.forEach((_, oldSet) -> {
            var newSet = oldSet.stream()
                    .filter(rule -> rules.containsKey(rule.id()))
                    .peek(rule -> {
                        var newRule = rules.get(rule.id()).clone();
                        rule.refreshRule(newRule);
                    }).collect(Collectors.toSet());

            oldSet.clear();
            oldSet.addAll(newSet);
        });
    }

    public void initPersistentData(MinecraftServer server) {
        enabledRules.clear();

        var dir = getPersistentDataDir(server);

        if (!Files.isDirectory(dir)) {
            CustomCoyoteControl.LOGGER.error("Data folder is not a directory!");
            try {
                Files.deleteIfExists(dir);
                Files.createDirectories(dir);
            } catch (IOException e) {
                CustomCoyoteControl.LOGGER.error("Failed to create data folder!", e);
            }
            return;
        }

        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public @NonNull FileVisitResult visitFile(
                        @NotNull Path file,
                        @NonNull BasicFileAttributes attrs
                ) throws IOException {
                    if (Files.isRegularFile(file)
                            && file.getFileName().toString().endsWith(".json")) {
                        try (var is = Files.newInputStream(file);
                             var reader = new InputStreamReader(is)) {
                            var json = JsonParser.parseReader(reader);
                            if (json != null && json.isJsonArray()) {
                                var containers = new HashSet<RuleContainer>();

                                for (var entry : json.getAsJsonArray()) {
                                    if (entry.isJsonObject()) {
                                        var object = entry.getAsJsonObject();

                                        var id = object.get("id").getAsString();
                                        var lastTriggerTime = object.get("last_trigger_time").getAsLong();
                                        var triggered = object.get("triggered").getAsBoolean();

                                        var rule = rules.get(id);
                                        if (rule == null) {
                                            CustomCoyoteControl.LOGGER.error("Failed to load rules for id: {}", id);
                                            continue;
                                        }

                                        var statesObjs = object.get("criterion_states").getAsJsonObject();
                                        for (var stateEntry : statesObjs.entrySet()) {
                                            var criterionId = stateEntry.getKey();
                                            var state = stateEntry.getValue();

                                            if (state.isJsonObject()) {
                                                var criterion = rule.criteriaWithIds().get(criterionId);

                                                if (criterion != null) {
                                                    criterion.deserializeState(state.getAsJsonObject());
                                                }
                                            }
                                        }

                                        var container = new RuleContainer(
                                                rule, id,
                                                lastTriggerTime, triggered
                                        );

                                        containers.add(container);
                                    }
                                }

                                var uuid = UUID.fromString(removeExtension(file.toString()));
                                enabledRules.put(uuid, containers);
                            }
                        }
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            CustomCoyoteControl.LOGGER.error("Error while loading data folder!", e);
            enabledRules.clear();
        }
    }

    public void savePersistentData(MinecraftServer server) {
        var dir = getPersistentDataDir(server);

        if (Files.isDirectory(dir)) {
            for (var entry : enabledRules.entrySet()) {
                var array = new JsonArray();

                for (var rule : entry.getValue()) {
                    var object = new JsonObject();

                    object.addProperty("id", rule.id());
                    object.addProperty("last_trigger_time", rule.lastTriggerTime());
                    object.addProperty("triggered", rule.triggered());

                    var stateObjs = new JsonObject();
                    for (var criterionEntry : rule.rule().criteriaWithIds().entrySet()) {
                        var criterionId = criterionEntry.getKey();
                        var criterion = criterionEntry.getValue();

                        var stateObj = criterion.serializeState();

                        stateObjs.add(criterionId, stateObj);
                    }
                    object.add("criterion_states", stateObjs);

                    array.add(object);
                }

                try (var writer = Files.newBufferedWriter(dir)) {
                    GSON.toJson(array, writer);
                } catch (IOException e) {
                    CustomCoyoteControl.LOGGER.error("Error while saving data folder!", e);
                }
            }
        }
    }

    private Path getRootPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve(".ccc");
    }

    private Path getRulesDir(MinecraftServer server) {
        return getRootPath(server).resolve("rules");
    }

    private Path getRuleSetsDir(MinecraftServer server) {
        return getRulesDir(server).resolve("rulesets");
    }

    private Path getPersistentDataDir(MinecraftServer server) {
        return getRootPath(server).resolve("data");
    }

    private static String removeExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }

        var lastDot = filePath.lastIndexOf('.');
        var lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

        if (lastDot > lastSeparator) {
            return filePath.substring(0, lastDot);
        }

        return filePath;
    }
}
