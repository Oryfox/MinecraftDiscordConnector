package de.oryfox;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {

    MinecraftDiscordConnector sender;

    public EventListener(MinecraftDiscordConnector sender) {
        this.sender = sender;
    }

    @EventHandler
    public void playerConnect(PlayerJoinEvent event) {
        sender.send(String.format("%s hat den Server betreten.", event.getPlayer().getDisplayName()));
    }

    @EventHandler
    public void playerDisconnect(PlayerQuitEvent event) {
        sender.send(String.format("%s hat den Server verlassen.", event.getPlayer().getDisplayName()));
    }

    @EventHandler
    public void playerMessage(AsyncPlayerChatEvent event) {
        sender.send(String.format("<%s>%s", event.getPlayer().getDisplayName(), event.getMessage()));
    }
}