package com.discordannouncements;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface DiscordAnnouncementsConfig extends Config
{
    @ConfigItem(
            keyName = "webhookUrl",
            name = "Discord Webhook URL",
            description = "Paste your Discord webhook URL here"
    )
    default String webhookUrl()
    {
        return "";
    }

    @ConfigItem(
            keyName = "testMessage",
            name = "Test Message",
            description = "Message to send when running ::test"
    )
    default String testMessage()
    {
        return "Test message from RuneLite DiscordAnnouncements plugin.";
    }
}
