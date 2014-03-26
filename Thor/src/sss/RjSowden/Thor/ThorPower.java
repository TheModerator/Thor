/* ThorPower.java
 *  created by Filbert66
 * History
 *  24 Oct 2013:  Integrated changes from "1.75" (3.1) from RjSowden;
 *				  Added PIGZOMBIE, WITHER, BAT; added LivingEntity.class
 *  25 Nov 2013 : Added WITCH, SPIDERs, BLAZE.
 *  07 Mar 2014 : Added Groundpound
 *  25 Mar 2014 : Added Arrows
 */

package sss.RjSowden.Thor;

import org.bukkit.entity.Bat;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Player;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;

public enum ThorPower {
	LIGHTNING(1, "lightning", "send lightning"),
	EXPLODE(2, "explode", "cause an explosion"),
	WOLF(3, "wolves", "spawn wolves", Wolf.class),
	TELEPORT(4, "teleport", "teleport to location"),
	FIREBALL(5, "fireball", "throw a fireball"),
	FIRE(6, "fire", "set on fire"),
	CREEPER(7,"creeper", "spawn creepers", Creeper.class),
	SMALLFIREBALL(8, "napalm", "throw a small fireball"),
	DELETE(9,"delete", "delete the target block"), // pick
	POWER(10,"power", "toggle redstone power"), // redstone torch?
	ZOMBIE(11,"zombie", "spawn zombies", Zombie.class),
	ENDERMAN(12,"enderman", "spawn endermen", Enderman.class),
	DRAGON(13,"dragon", "spawn a dragon", EnderDragon.class),
	SKELETON(14,"skeleton", "spawn skeletons", Skeleton.class),
	VILLAGER(15,"villager", "spawn villagers", Villager.class),
	COW(16,"cow", "spawn cows", Cow.class),
	PIGZOMBIE (17, "pigman", "spawn zombie pigmen", PigZombie.class),
	WITHER (18, "wither", "spawn a wither", Wither.class), 
	BAT (19, "bats", "spawn bats", Bat.class),
	WITCH (20, "witch", "spawn a witch", Witch.class),
	SPIDER (21, "spider", "spawn a spider", Spider.class),
	CAVESPIDER (22, "cavespider", "spawn a cave spider", CaveSpider.class),
	BLAZE (23, "blaze", "spawn a blaze", Blaze.class),
	GROUND_POUND (24, "groundpound", "pound the ground to make blocks and entities fly"),
	ARROWS (25, "arrows", "shoots multiple arrows"),
	;
	
	private final int power;
	private final String name;
	private final String phrase;
	private final Class<? extends LivingEntity> creature;

	ThorPower (int power, String name, String phrase) { 
		this (power, name, phrase, null);
	}
	ThorPower (int power, String name, String phrase, final Class<? extends LivingEntity> creature) {
		this.power = power;
		this.name = name;
		this.phrase = phrase;
		this.creature = creature;
	}

	String getPhrase () { return this.phrase; }
	String getCommand() { return this.name; }
	final int getValue() { return this.power; }
	Class<? extends LivingEntity> getCreatureClass() { return this.creature; }

	boolean isCreature() { return this.creature != null; }
	
	public static final int count = values().length;

	public static String commandList = ""; 
	static {
		for (ThorPower p : values()) {
			commandList += p.name + ", ";
		}
	}


	// Not using this static array currently. Point was to allow
	//  switch (variable) case ThorPower.getPower (ThorPower.FIRE.getValue())
	//  but that is really ugly
	private final static ThorPower byId[] = new ThorPower[count + 1 /* zero unused */];
    static {  
		for (ThorPower p : values()) {
			if (byId.length > p.power)
				byId [p.power] = p;
		}
	}
	public static ThorPower getPower (final int id) {
		if (byId.length > id) 
			return byId [id];
		else
			return null;
	}
	public static ThorPower getPower (String name) {
		for (ThorPower p : values()) 
			if (p.name.equals (name))
				return p;
		return null;
	}
}