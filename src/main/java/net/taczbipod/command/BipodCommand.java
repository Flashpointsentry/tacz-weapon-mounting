package net.taczbipod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.taczbipod.BipodAllowlist;
import net.taczbipod.TaczBipod;
import net.taczbipod.network.BipodNetwork;

import java.util.Map;
import java.util.Set;

/**
 * Guns:        /bipod addheld | removeheld | add <gun> | remove <gun>
 * Attachments: /bipod attachment addheld | removeheld | add <attachment> | remove <attachment>
 * Both:        /bipod list | reload
 * Requires permission level 2 (operators).
 */
@Mod.EventBusSubscriber(modid = TaczBipod.MODID)
public final class BipodCommand {
    private BipodCommand() {}

    private static final SuggestionProvider<CommandSourceStack> ALL_GUNS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    TimelessAPI.getAllCommonGunIndex().stream().map(Map.Entry::getKey), builder);
    private static final SuggestionProvider<CommandSourceStack> LISTED_GUNS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(BipodAllowlist.get(), builder);
    private static final SuggestionProvider<CommandSourceStack> ALL_ATTACHMENTS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    TimelessAPI.getAllCommonAttachmentIndex().stream().map(Map.Entry::getKey), builder);
    private static final SuggestionProvider<CommandSourceStack> LISTED_ATTACHMENTS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(BipodAllowlist.getAttachments(), builder);

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("bipod")
                .requires(src -> src.hasPermission(2))
                // guns
                .then(Commands.literal("addheld").executes(ctx -> heldGun(ctx, true)))
                .then(Commands.literal("removeheld").executes(ctx -> heldGun(ctx, false)))
                .then(Commands.literal("add")
                        .then(Commands.argument("gun", ResourceLocationArgument.id()).suggests(ALL_GUNS)
                                .executes(ctx -> addGun(ctx.getSource(), ResourceLocationArgument.getId(ctx, "gun")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("gun", ResourceLocationArgument.id()).suggests(LISTED_GUNS)
                                .executes(ctx -> removeGun(ctx.getSource(), ResourceLocationArgument.getId(ctx, "gun")))))
                // attachments
                .then(Commands.literal("attachment")
                        .then(Commands.literal("addheld").executes(ctx -> heldAttachment(ctx, true)))
                        .then(Commands.literal("removeheld").executes(ctx -> heldAttachment(ctx, false)))
                        .then(Commands.literal("add")
                                .then(Commands.argument("attachment", ResourceLocationArgument.id()).suggests(ALL_ATTACHMENTS)
                                        .executes(ctx -> addAttachment(ctx.getSource(), ResourceLocationArgument.getId(ctx, "attachment")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("attachment", ResourceLocationArgument.id()).suggests(LISTED_ATTACHMENTS)
                                        .executes(ctx -> removeAttachment(ctx.getSource(), ResourceLocationArgument.getId(ctx, "attachment"))))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource()))));
    }

    // ---------- guns ----------

    private static int heldGun(CommandContext<CommandSourceStack> ctx, boolean adding) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Only players can use this. Use /bipod " + (adding ? "add" : "remove") + " <gun> instead."));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            src.sendFailure(Component.literal("Hold a TACZ gun in your main hand first."));
            return 0;
        }
        ResourceLocation id = gun.getGunId(stack);
        return adding ? addGun(src, id) : removeGun(src, id);
    }

    private static int addGun(CommandSourceStack src, ResourceLocation id) {
        if (TimelessAPI.getCommonGunIndex(id).isEmpty()) {
            src.sendFailure(Component.literal("No TACZ gun with id " + id + " is loaded. Check the spelling, or hold it and use /bipod addheld."));
            return 0;
        }
        if (BipodAllowlist.add(id)) {
            BipodNetwork.syncRulesToAll();
            src.sendSuccess(() -> name(id).copy().append(Component.literal(" now counts as having a bipod (cover + prone mounting)")), true);
            return 1;
        }
        src.sendFailure(Component.literal(id + " is already on the gun list."));
        return 0;
    }

    private static int removeGun(CommandSourceStack src, ResourceLocation id) {
        if (BipodAllowlist.remove(id)) {
            BipodNetwork.syncRulesToAll();
            src.sendSuccess(() -> Component.literal("Removed ").append(name(id)).append(" from the gun list"), true);
            return 1;
        }
        src.sendFailure(Component.literal(id + " isn't on the gun list."));
        return 0;
    }

    // ---------- attachments ----------

    private static int heldAttachment(CommandContext<CommandSourceStack> ctx, boolean adding) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Only players can use this. Use /bipod attachment " + (adding ? "add" : "remove") + " <attachment> instead."));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        IAttachment attachment = IAttachment.getIAttachmentOrNull(stack);
        if (attachment == null) {
            src.sendFailure(Component.literal("Hold a TACZ attachment item (not installed on a gun) in your main hand first."));
            return 0;
        }
        ResourceLocation id = attachment.getAttachmentId(stack);
        return adding ? addAttachment(src, id) : removeAttachment(src, id);
    }

    private static int addAttachment(CommandSourceStack src, ResourceLocation id) {
        if (TimelessAPI.getCommonAttachmentIndex(id).isEmpty()) {
            src.sendFailure(Component.literal("No TACZ attachment with id " + id + " is loaded. Check the spelling, or hold it and use /bipod attachment addheld."));
            return 0;
        }
        if (BipodAllowlist.addAttachment(id)) {
            BipodNetwork.syncRulesToAll();
            src.sendSuccess(() -> Component.literal("Any gun with ").append(name(id)).append(" installed now counts as having a bipod"), true);
            return 1;
        }
        src.sendFailure(Component.literal(id + " is already on the attachment list."));
        return 0;
    }

    private static int removeAttachment(CommandSourceStack src, ResourceLocation id) {
        if (BipodAllowlist.removeAttachment(id)) {
            BipodNetwork.syncRulesToAll();
            src.sendSuccess(() -> Component.literal("Removed ").append(name(id)).append(" from the attachment list"), true);
            return 1;
        }
        src.sendFailure(Component.literal(id + " isn't on the attachment list."));
        return 0;
    }

    // ---------- both ----------

    private static int list(CommandSourceStack src) {
        Set<ResourceLocation> guns = BipodAllowlist.get();
        Set<ResourceLocation> attachments = BipodAllowlist.getAttachments();
        if (guns.isEmpty() && attachments.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No guns have a bipod yet (every gun can still use corners). Hold a gun and run /bipod addheld, or hold an attachment and run /bipod attachment addheld."), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Guns with a built-in bipod (" + guns.size() + "):").withStyle(ChatFormatting.GOLD), false);
        for (ResourceLocation id : guns) {
            boolean loaded = TimelessAPI.getCommonGunIndex(id).isPresent();
            src.sendSuccess(() -> line(id, loaded), false);
        }
        src.sendSuccess(() -> Component.literal("Attachments that count as a bipod (" + attachments.size() + "):").withStyle(ChatFormatting.GOLD), false);
        for (ResourceLocation id : attachments) {
            boolean loaded = TimelessAPI.getCommonAttachmentIndex(id).isPresent();
            src.sendSuccess(() -> line(id, loaded), false);
        }
        return guns.size() + attachments.size();
    }

    private static int reload(CommandSourceStack src) {
        BipodAllowlist.load();
        BipodNetwork.syncRulesToAll();
        src.sendSuccess(() -> Component.literal("Reloaded config/taczbipod/allowed_guns.json ("
                + BipodAllowlist.get().size() + " guns, " + BipodAllowlist.getAttachments().size() + " attachments)"), true);
        return 1;
    }

    private static Component line(ResourceLocation id, boolean loaded) {
        return Component.literal(" - ").append(name(id))
                .append(loaded ? Component.empty() : Component.literal(" (not loaded)").withStyle(ChatFormatting.RED));
    }

    private static Component name(ResourceLocation id) {
        return Component.literal(id.toString()).withStyle(ChatFormatting.AQUA);
    }
}
