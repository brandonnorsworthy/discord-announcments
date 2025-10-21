package com.discordannouncements;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("example")
public interface DiscordAnnouncementsConfig extends Config {
    // Webhook config section
    @ConfigSection(
            name = "Webhook Settings",
            description = "The config for webhook content notifications",
            position = 0,
            closedByDefault = true
    )
    String webhookConfig = "webhookConfig";

    @ConfigItem(
            keyName = "webhookURLs",
            name = "Webhook URL(s)",
            description = "The Discord Webhook URL(s) to send messages to, separated by a newline.",
            section = webhookConfig,
            position = 0
    )
    String webhook();

    // Levels config section
    @ConfigSection(
            name = "Level",
            description = "The config for level notifications",
            position = 1,
            closedByDefault = true
    )
    String levelConfig = "levelConfig";

    @ConfigItem(
            keyName = "includeLevel",
            name = "Send Level Notifications",
            description = "Send messages when you level up a skill.",
            section = levelConfig,
            position = 1
    )
    default boolean includeLevelling() {
        return false;
    }

    @ConfigItem(
            keyName = "minimumLevel",
            name = "Minimum level",
            description = "Levels greater than or equal to this value will send a message.",
            section = levelConfig,
            position = 2
    )
    default int minLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "levelInterval",
            name = "Send every X levels",
            description = "Only levels that are a multiple of this value are sent. Level 99 will always be sent regardless of this value.",
            section = levelConfig,
            position = 3
    )
    default int levelInterval() {
        return 1;
    }

    @ConfigItem(
            keyName = "levelMessage",
            name = "Level Message",
            description = "Message to send to Discord on Level",
            section = levelConfig,
            position = 5
    )
    default String levelMessage() {
        return "$name has reached $skill level $level.";
    }

    @ConfigItem(
            keyName = "includeTotalLevelMessage",
            name = "Include total level with message",
            description = "Include total level in the message to send to Discord.",
            section = levelConfig,
            position = 7
    )
    default boolean includeTotalLevel() {
        return true;
    }

    @ConfigItem(
            keyName = "totalLevelMessage",
            name = "Total Level Message",
            description = "Message to send to Discord when Total Level is included.",
            section = levelConfig,
            position = 8
    )
    default String totalLevelMessage() {
        return " - Total Level: $total";
    }
    // End levelling config section

    // Questing config section
    @ConfigSection(
            name = "Questing",
            description = "The config for questing notifications",
            position = 2,
            closedByDefault = true
    )
    String questingConfig = "questingConfig";

    @ConfigItem(
            keyName = "includeQuests",
            name = "Send Quest Notifications",
            description = "Send messages when you complete a quest.",
            section = questingConfig
    )
    default boolean includeQuestComplete() {
        return false;
    }

    @ConfigItem(
            keyName = "questMessage",
            name = "Quest Message",
            description = "Message to send to Discord on Quest",
            section = questingConfig,
            position = 1
    )
    default String questMessage() {
        return "$name has completed a quest: $quest";
    }
    // End questing config section

    // Pet config section
    @ConfigSection(
            name = "Pets",
            description = "The config for pet notifications",
            position = 5,
            closedByDefault = true
    )
    String petConfig = "petConfig";

    @ConfigItem(
            keyName = "includePets",
            name = "Send Pet Notifications",
            description = "Send messages when you receive a pet.",
            section = petConfig
    )
    default boolean includePets() {
        return false;
    }

    @ConfigItem(
            keyName = "petMessage",
            name = "Pet Message",
            description = "Message to send to Discord on Pet",
            section = petConfig,
            position = 1
    )
    default String petMessage() {
        return "$name has just received a pet!";
    }
    // End Pet config section

    // Collection Log section
    @ConfigSection(
            name = "Collection logs",
            description = "The config for collection logs",
            position = 6,
            closedByDefault = true
    )
    String collectionLogsConfig = "collectionLogsConfig";

    @ConfigItem(
            keyName = "includeCollectionLogs",
            name = "Collection Log Notifications",
            description = "Message to send to Discord on collection logs completions",
            section = collectionLogsConfig,
            position = 1
    )
    default boolean includeCollectionLogs() {
        return false;
    }

    @ConfigItem(
            keyName = "collectionLogMessage",
            name = "Collection log Message",
            description = "Message to send to Discord on collection logs completions",
            section = collectionLogsConfig,
            position = 2
    )
    default String collectionLogMessage() {
        return "$name has just completed a collection log: $entry";
    }

    @ConfigItem(
            keyName = "sendCollectionLogScreenshot",
            name = "Include collection log screenshots",
            description = "Include a screenshot with the Discord notification when you fill a new collection log slot",
            section = collectionLogsConfig,
            position = 3
    )
    default boolean sendCollectionLogScreenshot() {
        return false;
    }
    // end Collection Log section

    // combat achievements section
    @ConfigSection(
            name = "Combat Achievements",
            description = "The config for combat achievements",
            position = 6,
            closedByDefault = true
    )
    String combatAchievementsConfig = "combatAchievementsConfig";

    @ConfigItem(
            keyName = "includeCombatAchievements",
            name = "Combat Achievements Notifications",
            description = "Message to send to Discord on combat achievements completions",
            section = combatAchievementsConfig,
            position = 1
    )
    default boolean includeCombatAchievements() {
        return false;
    }

    @ConfigItem(
            keyName = "combatAchievementsMessage",
            name = "Combat Achievement Message",
            description = "Message to send to Discord on combat achievements completions",
            section = combatAchievementsConfig,
            position = 2
    )
    default String combatAchievementsMessage() {
        return "$name has just completed a combat achievement: $achievement";
    }
    // end combat achievements section
}
