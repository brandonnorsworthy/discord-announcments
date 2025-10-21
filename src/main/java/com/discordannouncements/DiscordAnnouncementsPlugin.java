package com.discordannouncements;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Text;
import okhttp3.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
    name = "Discord Announcements"
)
public class DiscordAnnouncementsPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private DiscordAnnouncementsConfig config;
    @Inject private ClientThread clientThread;

    // For screenshots
    @Inject private OkHttpClient okHttpClient;
    @Inject private DrawManager drawManager;

    // Track last known real levels to detect actual level-ups
    private final Map<Skill, Integer> lastRealLevels = new EnumMap<>(Skill.class);

    // Notification pop-up sequencing
    private boolean notificationStarted = false;

    @Override
    protected void startUp() throws Exception
    {
        log.info("DiscordAnnouncements started!");
        if (client.getGameState() == GameState.LOGGED_IN || client.getGameState() == GameState.LOADING)
        {
            for (Skill s : Skill.values())
            {
                try { lastRealLevels.put(s, client.getRealSkillLevel(s)); }
                catch (Exception ignored) { }
            }
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("DiscordAnnouncements stopped!");
        lastRealLevels.clear();
        notificationStarted = false;
    }

    // ---------- Manual test command: now respects global screenshot toggle ----------
    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted)
    {
        final String command = commandExecuted.getCommand();
        if (!"webhook".equalsIgnoreCase(command)) { return; }

        final List<String> urls = parseWebhookUrls(config.webhook());
        if (urls.isEmpty())
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Please set your Discord Webhook URL in the plugin settings.", null);
            return;
        }

        String base = Optional.ofNullable(config.collectionLogMessage()).filter(s -> !s.isBlank())
                              .orElse("Test message from RuneLite DiscordAnnouncements plugin.");
        final String rsn = client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
            ? client.getLocalPlayer().getName() : "Player";
        final String content = base.replace("$name", rsn).replace("$entry", "potato");

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Sending test webhook...", null);
        postDiscordToAll(urls, content, config.attachScreenshots());
    }

    // ---------- Level-ups ----------
    @Subscribe
    public void onStatChanged(StatChanged statChanged)
    {
        if (!config.includeLevel()) { return; }

        final Skill skill = statChanged.getSkill();
        if (skill == null) { return; }

        final int current = client.getRealSkillLevel(skill);
        final int previous = lastRealLevels.getOrDefault(skill, current);

        if (current <= previous)
        {
            lastRealLevels.put(skill, current);
            return;
        }
        lastRealLevels.put(skill, current);

        final int min = Math.max(0, config.minimumLevel());
        final int interval = Math.max(1, config.levelInterval());
        if (current < min) { return; }

        final boolean milestone = current == 99 || (current % interval == 0);
        if (!milestone) { return; }

        final String playerName = client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
            ? client.getLocalPlayer().getName() : "Player";

        String content = Optional.ofNullable(config.levelMessage()).filter(s -> !s.isBlank())
            .orElse("$name has reached $skill level $level.");

        content = content
            .replace("$name", playerName)
            .replace("$skill", skill.getName())
            .replace("$level", Integer.toString(current));

        if (config.includeTotalLevelMessage())
        {
            final int total = computeTotalLevel();
            String totalMsg = Optional.ofNullable(config.totalLevelMessage()).orElse(" - Total Level: $total");
            content = content + totalMsg.replace("$total", Integer.toString(total));
        }

        final List<String> urls = parseWebhookUrls(config.webhook());
        if (urls.isEmpty()) { return; }

        postDiscordToAll(urls, content, config.attachScreenshots());
        clientThread.invokeLater(() ->
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "DiscordAnnouncements: Level notification sent.", null)
        );
    }

    private int computeTotalLevel()
    {
        int sum = 0;
        for (Skill s : Skill.values())
        {
            if (s == Skill.OVERALL) { continue; }
            try { sum += client.getRealSkillLevel(s); } catch (Exception ignored) { }
        }
        return sum;
    }

    // ---------- Collection Log & Combat Achievement detection ----------

    @Subscribe
    public void onScriptPreFired(ScriptPreFired e)
    {
        switch (e.getScriptId())
        {
            case ScriptID.NOTIFICATION_START:
                notificationStarted = true;
                break;

            case ScriptID.NOTIFICATION_DELAY:
                if (!notificationStarted) { return; }

                String topText = client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT);
                String bottomText = client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT);
                notificationStarted = false;

                if (topText == null || bottomText == null) { return; }

                // Collection Log
                if (config.includeCollectionLogs() && "Collection log".equalsIgnoreCase(topText))
                {
                    String entry = Text.removeTags(bottomText).trim();
                    if (entry.toLowerCase().startsWith("new item:"))
                    {
                        entry = entry.substring("new item:".length()).trim();
                    }
                    handleCollectionLog(entry);
                    return;
                }

                // Combat Achievement
                if (config.includeCombatAchievements()
                    && "Combat Task Completed!".equalsIgnoreCase(topText)
                    && client.getVarbitValue(Varbits.COMBAT_ACHIEVEMENTS_POPUP) == 0)
                {
                    String task = extractMiddleText(bottomText);
                    handleCombatAchievement(task);
                }
                break;

            default:
                // ignore
        }
    }

    private static String extractMiddleText(String s)
    {
        String[] parts = s.split("<.*?>");
        for (String p : parts)
        {
            String t = p.trim();
            if (!t.isEmpty())
            {
                return t.replaceAll("[:?]$", "");
            }
        }
        return net.runelite.client.util.Text.removeTags(s).trim();
    }

    private void handleCollectionLog(String entry)
    {
        final List<String> urls = parseWebhookUrls(config.webhook());
        if (urls.isEmpty()) { return; }

        final String rsn = client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
            ? client.getLocalPlayer().getName() : "Player";

        String content = Optional.ofNullable(config.collectionLogMessage()).filter(s -> !s.isBlank())
            .orElse("$name has just added to the collection log: $entry");
        content = content.replace("$name", rsn).replace("$entry", entry);

        postDiscordToAll(urls, content, config.attachScreenshots());
    }

    private void handleCombatAchievement(String task)
    {
        final List<String> urls = parseWebhookUrls(config.webhook());
        if (urls.isEmpty()) { return; }

        final String rsn = client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null
            ? client.getLocalPlayer().getName() : "Player";

        String content = Optional.ofNullable(config.combatAchievementsMessage()).filter(s -> !s.isBlank())
            .orElse("$name completed combat task: $achievement");
        content = content.replace("$name", rsn).replace("$achievement", task);

        postDiscordToAll(urls, content, config.attachScreenshots());
    }

    // ---------- Discord posting helpers (OkHttp multipart for screenshots) ----------

    private void postDiscordToAll(List<String> urls, String content, boolean attachScreenshot)
    {
        if (urls == null || urls.isEmpty()) { return; }

        for (String url : urls)
        {
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) { continue; }

            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload_json", "{\"content\":\"" + escapeJson(content) + "\"}");

            if (attachScreenshot)
            {
                drawManager.requestNextFrameListener(image ->
                {
                    byte[] png = toPngBytes((BufferedImage) image);
                    if (png != null)
                    {
                        bodyBuilder.addFormDataPart(
                            "file",
                            "image.png",
                            RequestBody.create(MediaType.parse("image/png"), png)
                        );
                    }
                    sendMultipart(httpUrl, bodyBuilder.build());
                });
            }
            else
            {
                sendMultipart(httpUrl, bodyBuilder.build());
            }
        }
    }

    private void sendMultipart(HttpUrl url, RequestBody body)
    {
        Request request = new Request.Builder().url(url).post(body).build();
        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override public void onFailure(Call call, java.io.IOException e)
            {
                log.warn("Discord webhook failed", e);
            }
            @Override public void onResponse(Call call, Response response)
            {
                response.close();
                if (!response.isSuccessful())
                {
                    log.warn("Discord webhook HTTP {}", response.code());
                }
            }
        });
    }

    private static String escapeJson(String s)
    {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static byte[] toPngBytes(BufferedImage img)
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private List<String> parseWebhookUrls(String raw)
    {
        if (raw == null) { return List.of(); }
        return Arrays.stream(raw.split("\\r?\\n"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }

    @Provides
    DiscordAnnouncementsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DiscordAnnouncementsConfig.class);
    }
}
