package com.discordannouncements;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
@PluginDescriptor(
    name = "Discord Announcements"
)
public class DiscordAnnouncementsPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private DiscordAnnouncementsConfig config;

    @Inject
    private ClientThread clientThread;

    @Override
    protected void startUp() throws Exception {
        log.info("DiscordAnnouncements started!");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("DiscordAnnouncements stopped!");
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted) {
        final String command = commandExecuted.getCommand();
        if (!"webhook".equalsIgnoreCase(command)) {
            return;
        }

        final String webhookURLs = config.webhook();
        if (webhookURLs == null || webhookURLs.isBlank()) {
            client.addChatMessage(net.runelite.api.ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Please set your Discord Webhook URL in the plugin settings.", null);
            return;
        }

        String message = config.collectionLogMessage();

        //        return "$name has just completed a collection log: $entry";
        final String baseContent = message == null || message.isBlank()
        ? "Test message from RuneLite DiscordAnnouncements plugin."
        : message;
            // Perform simple placeholder replacements for the test command.
        final String rsn = client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
            ? client.getLocalPlayer().getName()
            : "Player";
        final String content = baseContent
            .replace("$name", rsn)
            .replace("$entry", "potato");


        final String payload = "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}";

        // optional immediate feedback
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Sending test webhook...", null);

        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookURLs))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    clientThread.invokeLater(() ->
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Test webhook sent successfully.", null)
                    );
                } else {
                    log.warn("Discord webhook responded with status {} and body {}", response.statusCode(), response.body());
                    final int status = response.statusCode();
                    clientThread.invokeLater(() ->
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Webhook responded with status " + status, null)
                    );
                }
            } catch (Exception e) {
                log.error("Failed to send Discord webhook test", e);
                clientThread.invokeLater(() ->
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Failed to send webhook test. See logs.", null)
                );
            }
        }, "discord-announcements-webhook").start();
    }

    @Provides
    DiscordAnnouncementsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DiscordAnnouncementsConfig.class);
    }
}