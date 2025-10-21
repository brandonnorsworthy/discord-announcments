package com.discordannouncements;

import net.runelite.client.config.*;

@ConfigGroup("discordannouncements")
public interface DiscordAnnouncementsConfig extends Config
{
    // ============================================================
    // Webhook Configuration
    // ============================================================

    @ConfigItem(
        keyName = "webhook",
        name = "Discord Webhook URL(s)",
        description = "Enter one or more Discord webhook URLs, one per line."
    )
    String webhook();

    // ============================================================
    // Global Screenshot Toggle
    // ============================================================

    @ConfigItem(
        keyName = "attachScreenshots",
        name = "Attach Screenshots",
        description = "Attach a screenshot to ALL announcements (level-ups, collection log, combat tasks, test command)."
    )
    default boolean attachScreenshots() { return true; }

    // ============================================================
    // Level-Up Notifications
    // ============================================================

    @ConfigSection(
        name = "Level-Up Settings",
        description = "Configure when and how level-up notifications are sent.",
        position = 0
    )
    String levelSection = "levelSection";

    @ConfigItem(
        keyName = "includeLevel",
        name = "Send Level-Up Announcements",
        description = "Enable or disable level-up messages.",
        section = levelSection,
        position = 1
    )
    default boolean includeLevel() { return true; }

    @Range(min = 1)
    @ConfigItem(
        keyName = "levelInterval",
        name = "Level Interval",
        description = "Announce every N levels (e.g., every 5 levels). Always announces level 99.",
        section = levelSection,
        position = 2
    )
    default int levelInterval() { return 5; }

    @Range(min = 1, max = 99)
    @ConfigItem(
        keyName = "minimumLevel",
        name = "Minimum Level",
        description = "Only announce level-ups at or above this level.",
        section = levelSection,
        position = 3
    )
    default int minimumLevel() { return 50; }

    @ConfigItem(
        keyName = "levelMessage",
        name = "Level Message",
        description = "Message format. Supports: $name, $skill, $level.",
        section = levelSection,
        position = 4
    )
    default String levelMessage()
    {
        return "$name has reached $skill level $level.";
    }

    @ConfigItem(
        keyName = "includeTotalLevelMessage",
        name = "Include Total Level",
        description = "Also include a message showing the player’s total level.",
        section = levelSection,
        position = 5
    )
    default boolean includeTotalLevelMessage() { return false; }

    @ConfigItem(
        keyName = "totalLevelMessage",
        name = "Total Level Message",
        description = "Message appended when showing total level. Supports: $total.",
        section = levelSection,
        position = 6
    )
    default String totalLevelMessage()
    {
        return " - Total Level: $total";
    }

    // ============================================================
    // Collection Log Notifications
    // ============================================================

    @ConfigSection(
        name = "Collection Log Settings",
        description = "Configure how collection log notifications are sent.",
        position = 10
    )
    String collectionLogSection = "collectionLogSection";

    @ConfigItem(
        keyName = "includeCollectionLogs",
        name = "Send Collection Log Notifications",
        description = "Announce when a new item is added to your collection log.",
        section = collectionLogSection,
        position = 11
    )
    default boolean includeCollectionLogs() { return true; }

    @ConfigItem(
        keyName = "collectionLogMessage",
        name = "Collection Log Message",
        description = "Message format. Supports: $name, $entry.",
        section = collectionLogSection,
        position = 12
    )
    default String collectionLogMessage()
    {
        return "$name has just added to the collection log: $entry";
    }

    // ============================================================
    // Combat Achievement Notifications
    // ============================================================

    @ConfigSection(
        name = "Combat Achievement Settings",
        description = "Configure how combat achievement notifications are sent.",
        position = 20
    )
    String combatSection = "combatSection";

    @ConfigItem(
        keyName = "includeCombatAchievements",
        name = "Send Combat Achievement Notifications",
        description = "Announce when a combat task or achievement is completed.",
        section = combatSection,
        position = 21
    )
    default boolean includeCombatAchievements() { return true; }

    @ConfigItem(
        keyName = "combatAchievementsMessage",
        name = "Combat Achievement Message",
        description = "Message format. Supports: $name, $achievement.",
        section = combatSection,
        position = 22
    )
    default String combatAchievementsMessage()
    {
        return "$name completed combat task: $achievement";
    }
}
