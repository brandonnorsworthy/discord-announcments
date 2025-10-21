package com.discordannouncements;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DiscordAnnouncementsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DiscordAnnouncementsPlugin.class);
		RuneLite.main(args);
	}
}