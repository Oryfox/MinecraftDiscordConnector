package de.oryfox;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.event.ListenerManager;

import java.util.Collections;

public class MinecraftDiscordConnector extends JavaPlugin {

    private String channelId;
    private String token;
    private DiscordApi discordApi;
    private ListenerManager<MessageCreateListener> listener;
    private boolean allowWhitelist = false;

    @Override
    public void onEnable() {
        var config = this.getConfig();
        if (config.contains("channelId")) {
            channelId = config.getString("channelId");
        } else {
            getLogger().info("Specify a channel id for discord using /setchannel");
        }
        if (config.contains("token")) {
            token = config.getString("token");
        } else {
            getLogger().info("Specify a discord bot token using /settoken");
        }
        if (config.contains("remoteWhitelist")) {
            allowWhitelist = config.getBoolean("remoteWhitelist");
            getLogger().info("Remote whitelist (/remoteWhitelist <true/false>) is " + (allowWhitelist ? "enabled" : "disabled and thus no slash commands will be registered."));
        }

        this.getCommand("setchannel").setExecutor((commandSender, command, s, strings) -> {
            config.set("channelId", strings[0]);
            saveConfig();
            return true;
        });

        this.getCommand("settoken").setExecutor((commandSender, command, s, strings) -> {
            config.set("token", strings[0]);
            saveConfig();
            return true;
        });

        this.getCommand("remotewhitelist").setExecutor(((commandSender, command, s, strings) -> {
            if (strings.length == 1) {
                if (strings[0].equalsIgnoreCase("true")) {
                    allowWhitelist = true;
                    config.set("remoteWhitelist", true);
                    saveConfig();
                    return true;
                } else if (strings[0].equalsIgnoreCase("false")) {
                    allowWhitelist = false;
                    config.set("remoteWhitelist", false);
                    saveConfig();
                    return true;
                } else {
                    commandSender.sendMessage("Invalid argument. Use true/false.");
                    return false;
                }
            } else {
                commandSender.sendMessage("Invalid argument count. Use true/false.");
                return false;
            }
        }));

        if (token != null && channelId != null) {
            getLogger().info("Trying to connect to channel with id " + channelId + "...");
            discordApi = new DiscordApiBuilder().setToken(token).setAllIntents().login().join();
            var channel = discordApi.getChannelById(channelId).orElseThrow(() -> new RuntimeException(String.format("Channel %s not found.", channelId)))
                    .asServerTextChannel().orElseThrow(() -> new RuntimeException(String.format("Channel %s is not a server text channel.", channelId)));
            listener = channel.addMessageCreateListener(evt -> {
                if (!evt.getMessageAuthor().isYourself()) {
                    Bukkit.broadcastMessage(String.format("<%s*>%s", evt.getMessageAuthor().getDisplayName(), evt.getMessage().getContent()));
                }
            });

            if (allowWhitelist) {
                SlashCommand.with("whitelist", "Whitelist a player", Collections.singletonList(SlashCommandOption.create(SlashCommandOptionType.STRING, "player", "The player to whitelist")))
                        .createGlobal(discordApi)
                        .join();
                SlashCommand.with("unwhitelist", "Unwhitelist a player", Collections.singletonList(SlashCommandOption.create(SlashCommandOptionType.STRING, "player", "The player to unwhitelist")))
                        .createGlobal(discordApi)
                        .join();

                discordApi.addSlashCommandCreateListener(event -> {
                    switch (event.getSlashCommandInteraction().getCommandName()) {
                        case "whitelist" -> {
                            var player = event.getSlashCommandInteraction().getArgumentStringValueByName("player").orElseThrow();
                            Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("whitelist add %s", player)));
                            event.getSlashCommandInteraction().createImmediateResponder().setContent(String.format("Whitelisted %s", player)).setFlags(MessageFlag.EPHEMERAL).respond();
                        }
                        case "unwhitelist" -> {
                            var player = event.getSlashCommandInteraction().getArgumentStringValueByName("player").orElseThrow();
                            Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("whitelist remove %s", player)));
                            event.getSlashCommandInteraction().createImmediateResponder().setContent(String.format("Unwhitelisted %s", player)).setFlags(MessageFlag.EPHEMERAL).respond();
                        }
                    }
                });
            }

            channel.sendMessage("The Server is now online!");
            getLogger().info("Successfully connected!");
            this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        }
    }

    @Override
    public void onDisable() {
        if (discordApi != null) {
            discordApi.getChannelById(channelId).flatMap(Channel::asServerTextChannel).ifPresent(serverTextChannel -> serverTextChannel.sendMessage("The Server is now offline!").join());
            listener.remove();
            discordApi.disconnect().join();
        }
    }

    public void send(String message) {
        if (discordApi != null) {
            discordApi.getChannelById(channelId)
                    .orElseThrow()
                    .asServerTextChannel()
                    .orElseThrow()
                    .sendMessage(message);
        }
    }
}