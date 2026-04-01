package com.awakened;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.OverheadTextChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Random;
import java.util.Set;

@Singleton
public class NpcOverheadManager
{
	private static final Set<Integer> DIALOGUE_NPC_IDS = Set.of(12429, 12433, 12434, 12289);

	private static final String[] CUSTOM_NPC_PHRASES = {
		"Oh my goodness, doth my eyes deceive me? Where did you find that cube.... I heard rumors but I didn't think they were true... I cannot allow you to awaken Mega Awakened Vardorvis. Hand that evil relic over immediately!",
		"An Awakener's... Cube?? How dare you bring such a disgusting object into this sacred realm. I will show you no mercy for your disgraceful actions. How can you live with yourself?!",
		"*glances nervously at the massive cube in your inventory* I thought dukey wukey was protecting that....",
		"I thought our people safely hid that away with the master's apprentice, young Mister Jekyll. What have you done to him? He would never choose to part with that cube, he understands what's at stake. Stop right there!",
		"yo I swear the Awakener's Cube was left near the Duke of Sucellus.. Or is this another one?"
	};
	private static final int[] PHRASE_WEIGHTS = { 3, 3, 1, 3, 3 };
	private static final int PHRASE_WEIGHT_TOTAL = 13;

	private static final String[] VARDORVIS_DEATH_PHRASES = {
		"noob", "lol u died", "bye", "pce", "moo...", "sit.",
		"How disappointing...", "I knew you weren't the one yea",
		"Your lack of coordination is scary u should work on that",
		"I was just getting into my rhythm..."
	};

	private static final Random RANDOM = new Random();

	private final Client client;
	private final AwakenedConfig config;

	private NPC acidTextNpc = null;
	private int acidTextTicksRemaining = 0;

	@Inject
	public NpcOverheadManager(Client client, AwakenedConfig config)
	{
		this.client = client;
		this.config = config;
	}

	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		Actor actor = event.getActor();
		if (actor instanceof NPC && DIALOGUE_NPC_IDS.contains(((NPC) actor).getId()))
		{
			actor.setOverheadText("<col=EA6464>" + getWeightedRandomPhrase());
		}
	}

	public void showAcidPhaseText(NPC npc)
	{
		String text = config.acidPhaseText();
		if (text != null && !text.isEmpty())
		{
			npc.setOverheadText(text);
			acidTextNpc = npc;
			acidTextTicksRemaining = 6;
		}
	}

	public void showVardorvisDeathRoast()
	{
		for (NPC npc : client.getNpcs())
		{
			if (npc.getId() == net.runelite.api.gameval.NpcID.VARDORVIS)
			{
				npc.setOverheadText(VARDORVIS_DEATH_PHRASES[RANDOM.nextInt(VARDORVIS_DEATH_PHRASES.length)]);
				break;
			}
		}
	}

	public void tick()
	{
		if (acidTextTicksRemaining > 0 && --acidTextTicksRemaining == 0 && acidTextNpc != null)
		{
			acidTextNpc.setOverheadText("");
			acidTextNpc = null;
		}
	}

	public void reset()
	{
		acidTextNpc = null;
		acidTextTicksRemaining = 0;
	}

	private static String getWeightedRandomPhrase()
	{
		int roll = RANDOM.nextInt(PHRASE_WEIGHT_TOTAL);
		int cumulative = 0;
		for (int i = 0; i < CUSTOM_NPC_PHRASES.length; i++)
		{
			cumulative += PHRASE_WEIGHTS[i];
			if (roll < cumulative)
			{
				return CUSTOM_NPC_PHRASES[i];
			}
		}
		return CUSTOM_NPC_PHRASES[0];
	}
}
