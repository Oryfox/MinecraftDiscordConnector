package de.oryfox;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.event.ListenerManager;

public class MinecraftDiscordConnector extends JavaPlugin {

    private String channelId;
    private String token;
    private DiscordApi discordApi;
    private ListenerManager<MessageCreateListener> listener;

    @Override
    public void onEnable() {
        var config = this.getConfig();
        if (config.contains("channelId")) {
            channelId = config.getString("channelId");
        } else {
            System.out.println("Specify a channel id for discord using /setchannel");
        }
        if (config.contains("token")) {
            token = config.getString("token");
        } else {
            System.out.println("Specify a discord bot token using /settoken");
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

        if (token != null && channelId != null) {
            System.out.printf("Trying to connect to channel with id %s...\n", channelId);
            discordApi = new DiscordApiBuilder().setToken(token).login().join();
            var channel = discordApi.getChannelById(channelId).orElseThrow(() -> new RuntimeException(String.format("Channel %s not found.", channelId)))
                    .asServerTextChannel().orElseThrow(() -> new RuntimeException(String.format("Channel %s is not a server text channel.", channelId)));
            listener = channel.addMessageCreateListener(evt -> {
                        if (!evt.getMessageAuthor().isYourself()) {
                            Bukkit.broadcastMessage(String.format("<%s*>%s", evt.getMessageAuthor().getDisplayName(), evt.getMessage().getContent()));
                        }
                    });
            channel.sendMessage("The Server is now online!");
            System.out.println("Successfully connected!");
            this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        }
    }

    @Override
    public void onDisable() {
        discordApi.getChannelById(channelId).flatMap(Channel::asServerTextChannel).ifPresent(serverTextChannel -> serverTextChannel.sendMessage("The Server is now offline!").join());
        listener.remove();
        discordApi.disconnect().join();
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