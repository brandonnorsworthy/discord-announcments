package com.discordannouncements;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.StatChanged;
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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // Track last known real levels to detect actual level-ups
    private final Map<Skill, Integer> lastRealLevels = new EnumMap<>(Skill.class);

    @Override
    protected void startUp() throws Exception {
        log.info("DiscordAnnouncements started!");
        // Initialize last known real levels
        if (client.getGameState() == GameState.LOGGED_IN || client.getGameState() == GameState.LOADING) {
            for (Skill s : Skill.values()) {
                try {
                    lastRealLevels.put(s, client.getRealSkillLevel(s));
                } catch (Exception ignored) {
                    // Some clients may not return values for pseudo skills
                }
            }
        }
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

        // Use the first valid URL for the test command to avoid spamming multiple channels
        final List<String> urls = parseWebhookUrls(webhookURLs);
        if (urls.isEmpty()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: No valid webhook URL found.", null);
            return;
        }

        final String testUrl = urls.get(0);

        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(testUrl))
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

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        if (!config.includeLevel()) {
            return;
        }

        final Skill skill = statChanged.getSkill();
        if (skill == null) {
            return;
        }

        // Real level to avoid boosted/fluctuating levels
        final int current = client.getRealSkillLevel(skill);
        final int previous = lastRealLevels.getOrDefault(skill, current);

        if (current <= previous) {
            lastRealLevels.put(skill, current);
            return; // no level up
        }

        // Update stored level immediately to prevent duplicate sends
        lastRealLevels.put(skill, current);

        // Apply config gates
        final int min = Math.max(0, config.minimumLevel());
        final int interval = Math.max(1, config.levelInterval());

        if (current < min) {
            return;
        }

        final boolean milestone = current == 99 || (current % interval == 0);
        if (!milestone) {
            return;
        }

        // Build message
        final String playerName = client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
                ? client.getLocalPlayer().getName()
                : "Player";

        String content = config.levelMessage();
        if (content == null || content.isBlank()) {
            content = "$name has reached $skill level $level.";
        }

        content = content
                .replace("$name", playerName)
                .replace("$skill", skill.getName())
                .replace("$level", Integer.toString(current));

        if (config.includeTotalLevelMessage()) {
            final int total = computeTotalLevel();
            String totalMsg = config.totalLevelMessage();
            if (totalMsg == null) {
                totalMsg = " - Total Level: $total";
            }
            totalMsg = totalMsg.replace("$total", Integer.toString(total));
            content = content + totalMsg;
        }

        final String payload = toJsonContentPayload(content);
        final List<String> urls = parseWebhookUrls(config.webhook());
        if (urls.isEmpty()) {
            return;
        }

        // Send to all configured URLs off the client thread
        for (String url : urls) {
            sendWebhookAsync(url, payload, "level-up");
        }

        clientThread.invokeLater(() ->
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Level notification sent.", null)
        );
    }

    private int computeTotalLevel() {
        int sum = 0;
        for (Skill s : Skill.values()) {
            if (s == Skill.OVERALL) {
                continue;
            }
            try {
                sum += client.getRealSkillLevel(s);
            } catch (Exception ignored) {
            }
        }
        return sum;
    }

    private List<String> parseWebhookUrls(String raw) {
        if (raw == null) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private String toJsonContentPayload(String content) {
        final String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"content\":\"" + escaped + "\"}";
    }

    private void sendWebhookAsync(String url, String payload, String tag) {
        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    log.warn("Discord webhook ({} {}) responded with status {} and body {}", tag, url, response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.error("Failed to send Discord webhook ({} {})", tag, url, e);
            }
        }, "discord-announcements-" + tag).start();
    }

    @Provides
    DiscordAnnouncementsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DiscordAnnouncementsConfig.class);
    }
}