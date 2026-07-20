package com.roserfwk.ccc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.roserfwk.ccc.data.rules.RuleManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

import java.util.concurrent.CompletableFuture;

public final class ControlCommands {
    private ControlCommands() {}

    public static final LiteralArgumentBuilder<CommandSourceStack> RULE_ENABLE = Commands.literal("enable")
            .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(SuggestionProviders::allRules)
                    .executes(ctx -> {
                        var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                        var ruleId = StringArgumentType.getString(ctx, "id");
                        ruleManager.enableRule(ctx.getSource().getPlayerOrException().getUUID(), ruleId);

                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("for").then(
                            Commands.argument("targets", EntityArgument.players())
                                    .executes(ctx -> {
                                        var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                                        var ruleId = StringArgumentType.getString(ctx, "id");
                                        EntityArgument.getPlayers(ctx, "targets").forEach(player ->
                                                ruleManager.enableRule(player.getUUID(), ruleId));

                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            );

    public static final LiteralArgumentBuilder<CommandSourceStack> RULE_DISABLE = Commands.literal("disable")
            .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(SuggestionProviders::allRules)
                    .executes(ctx -> {
                        var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                        var ruleId = StringArgumentType.getString(ctx, "id");
                        ruleManager.disableRule(ctx.getSource().getPlayerOrException().getUUID(), ruleId);

                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("for").then(
                                    Commands.argument("targets", EntityArgument.players())
                                            .executes(ctx -> {
                                                var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                                                var ruleId = StringArgumentType.getString(ctx, "id");
                                                EntityArgument.getPlayers(ctx, "targets").forEach(player ->
                                                        ruleManager.disableRule(player.getUUID(), ruleId));

                                                return Command.SINGLE_SUCCESS;
                                            })
                            )
                    )
            );

    public static final LiteralArgumentBuilder<CommandSourceStack> RULESET_ENABLE = Commands.literal("enable")
            .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(SuggestionProviders::allRules)
                    .executes(ctx -> {
                        var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                        var ruleId = StringArgumentType.getString(ctx, "id");
                        ruleManager.enableRuleSet(ctx.getSource().getPlayerOrException().getUUID(), ruleId);

                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("for").then(
                                    Commands.argument("targets", EntityArgument.players())
                                            .executes(ctx -> {
                                                var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                                                var ruleId = StringArgumentType.getString(ctx, "id");
                                                EntityArgument.getPlayers(ctx, "targets").forEach(player ->
                                                        ruleManager.enableRuleSet(player.getUUID(), ruleId));

                                                return Command.SINGLE_SUCCESS;
                                            })
                            )
                    )
            );

    public static final LiteralArgumentBuilder<CommandSourceStack> RULESET_DISABLE = Commands.literal("disable")
            .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(SuggestionProviders::allRules)
                    .executes(ctx -> {
                        var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                        var ruleId = StringArgumentType.getString(ctx, "id");
                        ruleManager.disableRuleSet(ctx.getSource().getPlayerOrException().getUUID(), ruleId);

                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("for").then(
                                    Commands.argument("targets", EntityArgument.players())
                                            .executes(ctx -> {
                                                var ruleManager = RuleManager.getInstance(ctx.getSource().getServer());
                                                var ruleId = StringArgumentType.getString(ctx, "id");
                                                EntityArgument.getPlayers(ctx, "targets").forEach(player ->
                                                        ruleManager.disableRuleSet(player.getUUID(), ruleId));

                                                return Command.SINGLE_SUCCESS;
                                            })
                            )
                    )
            );

    public static final LiteralArgumentBuilder<CommandSourceStack> RULE_ROOT =
            Commands.literal("rule").then(RULE_ENABLE).then(RULE_DISABLE);

    public static final LiteralArgumentBuilder<CommandSourceStack> RULESET_ROOT =
            Commands.literal("ruleset").then(RULESET_ENABLE).then(RULESET_DISABLE);

    public static final LiteralArgumentBuilder<CommandSourceStack> CCC_ROOT =
            Commands.literal("ccc").then(RULE_ROOT).then(RULESET_ROOT);

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, _, _) -> {
                    dispatcher.register(CCC_ROOT);
                });
    }

    static final class SuggestionProviders {
        public static CompletableFuture<Suggestions> allRules(
                CommandContext<CommandSourceStack> ctx,
                SuggestionsBuilder builder
        ) {
            var source = ctx.getSource();
            var ruleManager = RuleManager.getInstance(source.getServer());

            for (var ruleId : ruleManager.getRuleIds()) {
                builder.suggest(ruleId);
            }

            return builder.buildFuture();
        }
    }
}
