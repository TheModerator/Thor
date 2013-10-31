/* ThorPlayerListener.java
 *  extended from RjSowden by Filbert66, using new ThorPower enum
 * TODO:
 * History
 * 02 Nov 2012: Added DELETE, POWER. Collapsed WOLF & CREEPER.
 * 28 Nov 2012: Added CREEPER, SMALLFIREBALL powers, rewrote FIRE and FIREBALL
 * 24 Oct 2013: Began removing deprecated Bukkit calls. 
 *              Integrated changes from "1.75" (3.1) from RjSowden, fixing durabilityEnabled check.
 *              Added PigZombie, made angry; 
 *              added 0.0 format for cooldown, added cooldownEnabled check.
 */

package sss.RjSowden.Thor;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Effect;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;
import org.bukkit.ChatColor;

import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.util.List;
import java.util.Random;
import java.text.NumberFormat;

import sss.RjSowden.Thor.ThorPower;


public class ThorPlayerListener implements Listener{
	private final Thor plugin;
	private NumberFormat nf;

	public ThorPlayerListener (Thor instance)
	{
		this.plugin = instance;
		
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits (1);
	}

	@EventHandler (ignoreCancelled = false)
	public void onPlayerInteract(PlayerInteractEvent event){
		// Because you have to click AIR, cannot be within range of "touch". This is a good built-in 
		//  safety feature for dangerous actions.
		
		if ( !event.getPlayer().isSneaking() &&  // disables special effect, so can still right click with some things
		    ((event.getAction().equals(Action.LEFT_CLICK_AIR) && plugin.leftclick == true ) || 
			 (event.getAction().equals(Action.RIGHT_CLICK_AIR) && plugin.leftclick == false ) )) {
			Player player = event.getPlayer();
			ItemStack itemInHand = player.getItemInHand();
			
			if (plugin.matchesHammer (itemInHand)) { // must match ALL characteristics 
			  if (plugin.PlayerMode.containsKey(player) == true)
				usePower (player, plugin.PlayerMode.get(player));
			}	
			// Check power tool list and if present ignore PlayerMode
			else for (ThorPower power : ThorPower.values()) {
				if (plugin.matchesPowerWand (power, itemInHand)) {
					usePower (player, power);
					break;  // otherwise one cast could have multiple effects
				}
			}
		}
	}
	
	// Below function could be moved to ThorPower completely, removing "mode" param, but is it worth it?
	// RJ: Probs not, no worries.
	void usePower (Player player, ThorPower mode) {
		Location loc;
		int distance = plugin.getConfig().getInt (mode.getCommand() + ".distance");
		Random rand = new Random(); //randomize locations so mobs don't mirror one another.

		// Check cooldown
		long cooldown = 1000 * plugin.getConfig().getInt (mode.getCommand() + ".cooldown");
		if (plugin.cooldownEnabled && cooldown > 0) {
			Long lastTime = plugin.lastTimes.get (player);
			long currTime = System.currentTimeMillis();
			if (lastTime != null && lastTime.longValue() + cooldown >= currTime) {
				player.sendMessage ("Thor: You need to wait " + nf.format ((cooldown - (currTime - lastTime))/1000.0F) + 
									" seconds before casting again");
				return;
			}
		}	// write below all the time just so we can keep track of players using Thor.
		plugin.lastTimes.put (player, Long.valueOf(System.currentTimeMillis()));					
		
		// Impact Durability
		short durabilityImpact = (short) plugin.getConfig().getInt (mode.getCommand() + ".durability");
		// Should always get one based on having default values
		// RJ: Added a master command under GENERIC to switch it off/on should the user want to :)
		if (plugin.durabilityEnabled && durabilityImpact > 0) {
			loc = player.getLocation();
			ItemStack inHand = player.getItemInHand();
			
			int finalD = inHand.getDurability() + durabilityImpact;
			if (finalD >= inHand.getType().getMaxDurability())
			{  // for now, use durability of underlying item. Could make configurable for Thor's hammer
			   Location eye = player.getEyeLocation();
			   String itemName = inHand.getItemMeta().hasDisplayName() ?
			   		inHand.getItemMeta().getDisplayName() : null; // "the " + inHand.getType();
			   
			   if (inHand.getAmount() == 1)
			   		player.setItemInHand(null);
			   	else 
			   		inHand.setAmount (inHand.getAmount() - 1);
			   		
			   loc.getWorld().playEffect (eye, Effect.SMOKE, 0);
			   loc.getWorld().playSound (loc, Sound.ITEM_BREAK, 5F, 2F/*pitch*/);
			   if (itemName != null) {  // Only play sound and sendMessage if "magical"
				   loc.getWorld().playSound (loc, Sound.AMBIENCE_THUNDER, 1F, 0.5F/*pitch*/);
				   player.sendMessage ("Amazingly, " + ChatColor.LIGHT_PURPLE + itemName + ChatColor.RESET + " disappears with a puff of smoke");
			   }
			}
			else
				player.getItemInHand().setDurability ((short)finalD);
		}

		switch (mode) {	
			case LIGHTNING:
				loc = player.getTargetBlock(null, distance).getLocation();
				player.getWorld().strikeLightning(loc);
				break;
	
			case EXPLODE:
				loc = player.getTargetBlock(null, distance).getLocation();
				player.getWorld().createExplosion(loc, plugin.xPower.floatValue());
				break;
	
			case PIGZOMBIE: 
			case SKELETON:
			case CREEPER:
			default:
				if ( !mode.isCreature()) {
					plugin.log.warning ("unexpected power: " + mode);
					return;
				}
				Class creatureClass = mode.getCreatureClass();

				loc = player.getTargetBlock(null, distance).getLocation();
				loc.setY(loc.getY() + 2);
				for (int i = 0; i < plugin.getConfig().getInt(mode.getCommand() + ".quantity"); i++)
				{
					Location rLoc = loc;
					rLoc.setX(loc.getX() + (rand.nextFloat()*i - i/2));
					rLoc.setZ(loc.getZ() + (rand.nextFloat()*i - i/2));
					/* Idea: new command "spawn" which requires a creature param. Would have to store that 
					 *  creature type (converting Entity(String) match) in a global hashmap
					 * Then if "spawn" mode = <stored EntityClass>;
					 * 
					 * RJ: Naah, to complimicated, both for users and from a programming aspect (Entity casting ect...), as we may want animals to do/be custom things when they spawn. Have implemented  solution here.
					 */
					LivingEntity creature = (LivingEntity) player.getWorld().spawn (rLoc, creatureClass); 
					 
					try { 
						Method setAngry = creatureClass.getMethod ("setAngry", Boolean.TYPE);
						try {
							setAngry.invoke (creature, true);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} catch (NoSuchMethodException ex) {
					 	// fine, try to setTarget()
					} catch (SecurityException ex) {
						plugin.log.warning ("setAngry failed" + ex);
					}
					
					double rad = plugin.getConfig().getDouble (mode.getCommand() + ".targetingRange", 0);
					if (plugin.targetingEnabled && rad > 0) {
						/*  Should do _in addition_ to Angry, since angry wolves won't attack a player right off, until they have a target. 
						 *    Pigmen, on the other hand, attack and hurt without being angry or having a sword. 
						 *  Use new config item, targetRange. If 0, don't set.
						 */  
						Entity nearest = null;
						for (Entity e : creature.getNearbyEntities (rad, rad, rad)) {
						/*  nearest n, n, P, E, P, E
							new     P, E, P  P, E, E
							Result  O, O, d  O  x  d    Results: OK, dISTANCE, eXclude */
							if (e instanceof LivingEntity &&	// found a living Entity to target
								(nearest == null ||				// haven't found anything else yet
								 (!(nearest instanceof Player) && e instanceof Player) ||  //  Found first Player
								 (!(nearest instanceof Player && !(e instanceof Player)) && // don't care if non-Player is closer than a Player
								  rLoc.distance (e.getLocation()) < rLoc.distance (nearest.getLocation()) ) ) )
								  
							{
								plugin.log.fine ("Found better target than " + nearest + ", a " + e.getType());
								nearest = e;
								// Pick nearest Player, if not, nearest LivingEntity
							}						
						}
						if (nearest != null) {
							if (creature instanceof Monster || creature instanceof Wolf) {
								creature.setLastDamageCause (new EntityDamageByEntityEvent (nearest,  // damager
									creature, // damagee
									EntityDamageEvent.DamageCause.ENTITY_ATTACK, 0.0D ));

								/** Tried this and it's bad. Actually hurts them and causes them to phase into the ground.
								 * Supposed to trigger goal-setting code to one that "damaged" them.
								creature.damage (0D, nearest);  
								**/
							} else 
								plugin.log.info ("setTarget() of Animals doesn't work; vote up Bukkit-1358");
							
						/*** Doesn't work. See Bukkit-1358
							// call setTarget, but since not all creatures support this (ex. bat)....
							plugin.log.fine ("Nearest target to " + creature + " is a " + nearest.getType());
							try { 
								Method setTarget = creatureClass.getMethod ("setTarget", LivingEntity.class);
								try {
									setTarget.invoke (creature, nearest);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							} catch (NoSuchMethodException ex) {
								// fine, it might not support it
								plugin.log.warning ("setTarget (LivingEntity) doesn't exist");
							} catch (SecurityException ex) {
								plugin.log.warning ("setTarget failed" + ex);
							}
							***/
						}
						
					}
																
					// Unique settings
					if (mode == ThorPower.CREEPER)
						((Creeper)creature).setPowered (true);		
					if (mode == ThorPower.PIGZOMBIE)
						creature.getEquipment().setItemInHand (new ItemStack (Material.GOLD_SWORD));
					if (mode == ThorPower.SKELETON)
						creature.getEquipment().setItemInHand (new ItemStack (Material.BOW));
						
				}
				break;
			
			case TELEPORT:
				loc = player.getTargetBlock(null, distance).getLocation();
				loc.add(0, 3, 0);
				player.teleport(loc);
				break;
	
			case FIREBALL:
			case SMALLFIREBALL:											
				Fireball fireball;
				if (mode == ThorPower.SMALLFIREBALL)
					fireball= player.launchProjectile(SmallFireball.class);
				else
					fireball= player.launchProjectile(Fireball.class);
	
				fireball.setShooter (player);
				fireball.setFireTicks(9999);
				break;
	
			case FIRE:
				List <Block> targets = player.getLastTwoTargetBlocks (null, distance); //ignores air
				if (targets.size() != 2) {
					plugin.log.warning ("Unexpected size (" + targets.size() + ") from getLastTwoTargetBlocks");
					return;
				}
				Block TB = targets.get (1); // last item is target block
				Block before = targets.get (0);
				
				//if target is burnable and before is AIR
				if ( !TB.isEmpty() && !TB.isLiquid() && before.getType() == Material.AIR)
					before.setType (Material.FIRE);
				
				// Should try to burn an Entity if that is targeted, but that's difficult, and SMALLFIREBALL 
				//  does the trick, so not worth fixing. 
				return;     
				
			case DELETE:
				loc = player.getTargetBlock(null, distance).getLocation();
				loc.getBlock().setType (Material.AIR);
			    loc.getWorld().playEffect (loc, Effect.SMOKE, 0);
			    loc.getWorld().playSound (loc, Sound.CHICKEN_EGG_POP, 3F/*vol*/, 2F/*pitch*/); // RJ: LOVE the POP noise idea!
				// TODO RJ TASK: Add so if a PLAYER is targeted, the PLAYER is killed
			    
				break;
				
			case POWER:
				loc = player.getTargetBlock(null, distance).getLocation();
				Block b = loc.getBlock();
				Block redstone;
					if(b.getRelative(BlockFace.NORTH).getType() == Material.REDSTONE_WIRE){
						redstone = b.getRelative(BlockFace.NORTH);
					}else if(b.getRelative(BlockFace.SOUTH).getType() == Material.REDSTONE_WIRE ){
						redstone = b.getRelative(BlockFace.SOUTH);
					}else if(b.getRelative(BlockFace.EAST).getType() == Material.REDSTONE_WIRE ){
						redstone = b.getRelative(BlockFace.EAST);
					}else if(b.getRelative(BlockFace.WEST).getType() == Material.REDSTONE_WIRE ){
						redstone = b.getRelative(BlockFace.WEST);
					}else if(b.getType() == Material.REDSTONE_WIRE ){
						redstone = b.getRelative(BlockFace.WEST);
					}else{
						player.sendMessage("Could not find redstone... Power not changed");
						return;
					}
				if (b.getBlockPower() == 0){					
					
					b.setData((byte) 0xF, true);
					player.sendMessage("Power switched " + ChatColor.GREEN + "ON");
				}else{
				
					b.setData((byte) 0x0, true);
					player.sendMessage("Power switched " + ChatColor.RED + "OFF");
				}
				break;
				
		}	
	}
}
