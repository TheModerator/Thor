/* ThorPlayerListener.java
 *  extended from RjSowden by Filbert66, using new ThorPower enum
 * TODO:
 * History
 * 02 Nov 2012: Added DELETE, POWER. Collapsed WOLF & CREEPER.
 * 28 Nov 2012: Added CREEPER, SMALLFIREBALL powers, rewrote FIRE and FIREBALL
 * 		    RJ: Added a master command under GENERIC to switch it off/on should the user want to :)
 * 24 Oct 2013: Began removing deprecated Bukkit calls. 
 *              Integrated changes from "1.75" (3.1) from RjSowden, fixing durabilityEnabled check.
 *              Added PigZombie, made angry; 
 *              added 0.0 format for cooldown, added cooldownEnabled check.
 * 01 Nov 2013: allow for DURABILITY enchantment reducing usage; 
 * 07 Mar 2014 : Added Groundpound
 * 25 Mar 2014 : Added Arrows and sound for fireballs.
 */

package sss.RjSowden.Thor;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Effect;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.bukkit.World;

import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.text.NumberFormat;

import sss.RjSowden.Thor.ThorPower;


public class ThorPlayerListener implements Listener{
	private final Thor plugin;
	private NumberFormat nf;
	private static Random rand = new Random(); 

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
		}	
		// Store last use at end of this function, in case use fails.
		
		// Impact Durability
		short durabilityImpact = (short) plugin.getConfig().getInt (mode.getCommand() + ".durability");
		// Take into account Durability Enchantment
		ItemStack inHand = player.getItemInHand();
		int durabilityEnchant = inHand.getItemMeta().getEnchantLevel (Enchantment.DURABILITY);
		if (durabilityEnchant > 0 && 100/(durabilityEnchant + 1) <= rand.nextInt(100)) {
			durabilityImpact = 0;
			plugin.log.fine ("Durability enchantment saved wear impact on " + mode + " power use");
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
				loc.add(0, 2, 0);
				// Error checking: when clicking in air, TP to Y=3 in earth! 
				if (loc.getBlock().getType().isSolid() || loc.clone().add (0,1,0).getBlock().getType().isSolid()) {
					player.sendMessage (ChatColor.RED + "[Thor] will not teleport you to your death into a solid");
					return;
				} else {
					player.teleport(loc);
					plugin.log.fine (player.getName() + " teleported to " + loc);
				}
				break;
	
			case FIREBALL:
			case SMALLFIREBALL:											
				Fireball fireball;
				if (mode == ThorPower.SMALLFIREBALL) {
					fireball= player.launchProjectile(SmallFireball.class);
					player.playEffect (player.getLocation(), Effect.BLAZE_SHOOT, 1); // what is last param?
				}
				else {
					fireball= player.launchProjectile(Fireball.class);
					player.playEffect (player.getLocation(), Effect.GHAST_SHOOT, 1); // what is last param?
				}	
				fireball.setShooter (player);
				fireball.setFireTicks(9999);
				break;
			
			case ARROWS:
				int max = plugin.getConfig().getInt(mode.getCommand() + ".quantity");
				final Player p = player;
				for (int i =0; i < max; i++)
				{
					class ArrowShooter extends BukkitRunnable {
						@Override
						public void run() {
							if ( !p.isOnline()) {
								return;
							}
							// launch without Vector uses player cursor loc
							Arrow a = p.launchProjectile (Arrow.class);
							if (a != null) {
								p.playEffect (p.getLocation(), Effect.BOW_FIRE, 1); // what is last param?
								 a.setCritical (true);
								 a.setShooter (p);	
								 a.setBounce (false);				
								 								 
								try {
									Method getHandleMethod = a.getClass().getMethod("getHandle");
									Object handle = getHandleMethod.invoke(a);
									Field fromPlayerField = handle.getClass().getField("fromPlayer");
									fromPlayerField.setInt(handle, 2); // from infinite bow
								} catch (Throwable ex) {
									ex.printStackTrace(); // Up to you to ignore this, might break (gracefully-ish) on new MC versions.
								}		
							}							
						}
					}
					// by delaying, we avoid arrows hitting one another and introduce natural spread
					(new ArrowShooter()).runTaskLater(this.plugin, i*10);	// 0.5s for each arrow
				}
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
				break;     
				
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
			
			case GROUND_POUND:
				loc = player.getTargetBlock(null, distance).getLocation();
				if (loc == null)
					return;
				/*** Based on scientific study of water droplets in water:
				    http://alexandria.tue.nl/openaccess/Metis133016.pdf
				      "ring-wave energy presents a very strong dependence on the drop size"
				      and showing peak slopes at 110xdrop-diameter distance from center 
				 * We should make it peak at 10 blocks away, then taper off
				 * and magnitude should be a bell curve across 0-20 blocks away.
				 * Should not impact pounder, but all else.
				 * Power 1 = peak at 1 block away, N at N^^2 blocks away. 
				 * depth of blocks affected = sqrt(N), with power underneath equal to layer/(depth from top)
				 * Peak for power N = N 
				 */
				double power = plugin.getConfig().getDouble (mode.getCommand() + ".power");
				double divisor = plugin.getConfig().getDouble (mode.getCommand() + ".divisor", 4.0D); // tuned by testing

				// plugin.log.info ("Ground Pound! power " + power + " at " + loc);
				double peakDistance = power; 
				double widthFactor = 0.5D * peakDistance; // inflection at peak +/- widthF
				widthFactor *= widthFactor * 2.0D; 
				double radius = power*2 + 1.0D; // twice power/peak away

				// throw entities up, before blocks so they aren't buried
				List <Entity> eList = getNearbyEntities (loc, radius);
				//plugin.log.info ("Starting with " + eList.size() + " entities in box");
				
				ListIterator<Entity> li = eList.listIterator();
				while (li.hasNext()) {
					Entity e = li.next();
					double edistance = loc.distance (e.getLocation());
					if (edistance > radius || (e.getType() == EntityType.PLAYER && player.equals (e))) { // outside range
						// prune those in corners of box
						// eList.remove (e);
						continue;
					}
					if (e.getType() == EntityType.FALLING_BLOCK || !e.isOnGround()) {
						if (e.getType() != EntityType.FALLING_BLOCK) {
						//	plugin.log.info ("Skipping " + e.getType() + " not on ground");
						}
						continue;
					}
					// Gaussian distribution : peak * exp ( - (x - peak)^2 / (2 * width^2) )
					// http://en.wikipedia.org/wiki/Gaussian_function
					double exponent = - (edistance - peakDistance)*(edistance- peakDistance) / widthFactor; 
					double upSpeed = (power / divisor) * Math.exp (exponent);
					// throw up
					e.setVelocity (new Vector (0, upSpeed, 0));
					//plugin.log.info ("Threw " + e.getType() + " at distance of " + edistance + " up with velocity " + upSpeed);
				}				

				// throw blocks up
				double maxDepth = Math.sqrt (power);
				// plugin.log.info ("Boosting blocks at maxDepth " + maxDepth);

				int i, j, k;
				for (i = 0; i < radius; i++)
					for (k = 0; k < radius; k++) {
						double ldistance = loc.distance (loc.clone().add (i, 0, k));
						if (ldistance > radius) {
							// prune those in corners of box
							// Note that corners are symmetric, so we can skip all with this
							continue;
						}
						
						for (j = 0; j < maxDepth; j++) {
							Location l = loc.clone().add (i,-j,k);	// call top down 							
							double exponent = - (ldistance - peakDistance)*(ldistance- peakDistance) / widthFactor; 
							// blocks are affected twice as much by same power, so halve max, almost
							double upSpeed = (power / (1.5D *divisor)) * Math.exp (exponent);
							// Each lower layer is boosted at a fraction of power, decreasing in depth
							upSpeed *= (maxDepth - j) / maxDepth;
							boostBlock (l, upSpeed);

							// boost +/- i,k
							if (k != 0) {
								l = loc.clone().add (i,-j,-k);
								// same distance from center
								boostBlock (l, upSpeed);
							}
							if (i != 0) {
								l = loc.clone().add (-i,-j, k);
								// same distance from center
								boostBlock (l, upSpeed);
							}							
							if (i != 0 && k != 0) {
								l = loc.clone().add (-i,-j,-k);
								// same distance from center
								boostBlock (l, upSpeed);
							}							
						}
					}
				
				break;
				
		}	
		// write below all the time just so we can keep track of players using Thor.
		plugin.lastTimes.put (player, Long.valueOf(System.currentTimeMillis()));					

		
		// Impact only if power succeeded.
		if (plugin.durabilityEnabled && durabilityImpact > 0) {
			loc = player.getLocation();
			
			int finalD = inHand.getDurability() + durabilityImpact;
			if (finalD >= inHand.getType().getMaxDurability())
			{  
			   Location eye = player.getEyeLocation();
			   String itemName = inHand.getItemMeta().hasDisplayName() ?
			   		inHand.getItemMeta().getDisplayName() : null; // "the " + inHand.getType();
			   
			   if (inHand.getAmount() == 1) {
				// BUG: Setting null to an enchanted sword, it still remains in player's hand. 
			   		player.setItemInHand(null);
			   		plugin.log.fine ("Set null to hand, now: " + player.getItemInHand ());
			   	}
			   	else {
			   		inHand.setAmount (inHand.getAmount() - 1);
					// plugin.log.fine ("Reduced count of magic item to " + inHand.getAmount());
				}
			   		
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
		
	}
	
	/* 
	 * Utility Functions
	 */
	private boolean isLightWeight (Material m) {
		switch (m) {
			case SAPLING:
			case LONG_GRASS:
			case DEAD_BUSH:
			case YELLOW_FLOWER:
			case RED_ROSE:
			case BROWN_MUSHROOM:
			case RED_MUSHROOM:
			case TORCH:
			case FIRE:
			case CROPS:
			case SIGN_POST:
			case LEVER:
			case SNOW:
			case STONE_BUTTON:
			case PUMPKIN_STEM:
			case MELON_STEM:
			case VINE:
			case WATER_LILY:
			case COCOA:
			case TRIPWIRE:
			case FLOWER_POT:
			case POTATO:
			case CARROT:
			case WOOD_BUTTON:
				return true;
			default:
				return false;
		}
	}

	 // boosts the block with vertical velocity unless above is not AIR
	private boolean boostBlock (Location loc, double velocity) {
		Block b = loc.getBlock();
		Material m = b.getType();
		byte data = b.getData();
		World w = loc.getWorld();
		Entity moving = null; 
		
		if (velocity < 0.04D)
			return false; // too heavy to push up
			
		Location above = loc.clone().add (0,1,0);
		Material aboveMat = above.getBlock().getType() ;
		if (aboveMat != Material.AIR && !isLightWeight (aboveMat)) {
		/** recurse isn't too slow, but is leaving holes
			velocity /= 2;
			if ( !boostBlock (above, velocity))
			// else go ahead and now boost current block

		 **/
			return false;
		}
		
		b.setType (Material.AIR); // remove it first to make room
		if (m == Material.TNT) {
			// throw primed TNT instead
			try {
				TNTPrimed tnt = w.spawn (loc, TNTPrimed.class);
				if (tnt == null) 
					plugin.log.warning ("Unable to spawn TNT at " + loc);
				else
					moving = tnt;
			} catch (Throwable t) {
				plugin.log.warning (t + ": Unable to spawn and trigger TNT at " + loc);
			}
		} else {
			FallingBlock falling = w.spawnFallingBlock (loc, m, data);
			if (falling == null)
				plugin.log.warning ("Unable to spawn moving block at " + loc);
			else {
				falling.setDropItem (false); // drop item if can't place
				moving = falling;
			}
		}	
		
		if (moving != null) {
			moving.setVelocity (new Vector (0, velocity, 0));
		}
		else {
			w.dropItem (loc, new ItemStack (m, 1, (short)0, data));
		}	
		return true;			
	}

	private List <Entity> getNearbyEntities (Location loc, double radius) {
		Chunk center = loc.getChunk();
		List <Entity> eList = new ArrayList ();
		int chunks = 1;
		
		// Process local chunk
		for (Entity e: center.getEntities()) {
			if (loc.distance (e.getLocation()) <= radius) { // within range
				eList.add (e);
			}
		}
		// Process nearby chunks (could be only +1 away!)
		double centerX = center.getX(), centerZ = center.getZ();
			
		for (double i = -radius ; i<radius; i += 16) // increment by chunk size
			for (double j = -radius; j < radius; j += 16) {
				Vector newL = loc.toVector().add (new Vector (i, 0, j));
			 	Chunk newChunk = newL.toLocation(loc.getWorld()).getChunk();
			 	if (newChunk.getX() != centerX || newChunk.getZ() != centerZ) {
			 		chunks++;													
					for (Entity e: newChunk.getEntities()) {
						if (loc.distance (e.getLocation()) <= radius) { // within range
							eList.add (e);
						}
					}
				}
			}
		//* DEBUG */ log.fine ("getNearbyEntities (loc, " + radius + ") processed " + chunks + " chunks, found " + eList.size() + " entities");
		return eList;
	}		

/***
	private int dropItems (ItemStack[] drops, World w, Location loc, int yield) {
		int totalDrops = 0;
		
		for (ItemStack drop : drops) {
			if (drop != null && drop.getAmount() > 0) {				
				/* DEBUG * plugin.log.finer ("AC: Destroying stack " + drop.toString());
				
				// check dropped items against creeper_yield

				int newAmount = drop.getAmount();

/***
				// modify amount in stack by yield. 
				//  Integer arithmatic will always make a quantity of 1 reduce to 0, so new rand each time
				for (int i =0; i< drop.getAmount(); i++)
					if (rng.nextInt (100) < yield)
						newAmount += 1;
				drop.setAmount (newAmount);
//***				
				if (newAmount > 0) // believe this will eliminate check contents if chest amount=0
					w.dropItemNaturally(loc, drop); 
				/*DEBUG * plugin.log.finer ("AC: Stack dropped " + newAmount + " blocks"); 
			}
		}
		return totalDrops;
	}		
	// Throw stacks of items naturally from w.loc with velocity v, modifying count by provided yield
	private int throwItems (ItemStack[] drops, World w, Location loc, Vector v, int yield) {
		int totalDrops = 0;
		int vertBoost = plugin.getConfig().getInt("physics_height",2);
		
		for (ItemStack drop : drops) {
			if (drop != null && drop.getAmount() > 0) {				
				//* DEBUG * plugin.log.finer ("AC: Destroying stack " + drop.toString());
				
				// check dropped items against yield

				int newAmount = drop.getAmount();

/***
				// modify amount in stack by yield. 
				//  Integer arithmatic will always make a quantity of 1 reduce to 0, so new rand each time
				for (int i =0; i< drop.getAmount(); i++)
					if (rng.nextInt (100) < yield)
						newAmount += 1;
				drop.setAmount (newAmount);
***
				totalDrops += newAmount;
				
				if (newAmount > 0) // believe this will eliminate check contents if chest amount=0
				{
					double x = loc.getX(), y = loc.getY() + 0.5 + vertBoost, z = loc.getZ();
					if (drop.getType().isBlock()) {
						FallingBlock fb = w.spawnFallingBlock (new Location (w,x,y,z), drop.getType(), drop.getData().getData()); 
						fb.setDropItem(true);
						fb.setVelocity (v);
					} else { // can't throw non-blocks; need to spawn as entities
						Item floater = w.dropItem (new Location (w,x,y,z), drop);
						floater.setVelocity (v);
					}
					plugin.log.finer ("throwing " + drop.getType() + " with velocity " + v);
				}
				//*DEBUG * plugin.log.finer ("AC: Stack throw " + newAmount + " blocks"); 
			}
		}
		return totalDrops;
	}		
	
	// Drops all the blocks, modified by yield, with supplied power
	private void vectorDrop (List <Block> damagedBlocks, World w, Location loc, int yield, float power) {
		// BlockIdList emptyBlocklist = new BlockIdList (this.plugin);
		
		// With an empty blacklist, all blocks will be processed
		filterDrop (damagedBlocks, /* emptyBlocklist, false /*blacklist,*s w, loc, yield, power);
	}
	
	/*
	 * Drops the list of blocks, count modified by the supplied yield, and rewrites their current location with AIR.
	 *   If provided filter is a whitelist, leaves alone blocks NOT on the list and damages/drops the ones on the list
	 *   If provided filter is a blacklist, leaves alone blocks on the list and damages/drops the ones NOT on the list
     *  
	 * If an affected block is a container, always drops 100% of container contents.
	 * 
	 * If power > 0, dropped blocks are assigned a velocity equal to power/distance^2 away from center.
     *
	private void filterDrop (List <Block> damagedBlocks, 
							 // BlockIdList blockFilter, Boolean ifwhitelist, 
							 World w, Location loc, int yield, float power)
	{
		// plugin.log.fine (": filtering drop of (" + damagedBlocks.size() + " blocks through " + blockFilter.name() + " with " + yield + "% yield)");
		int totalDrops = 0, totalSafe = 0;
		int tntTicks = plugin.getConfig().getInt ("nerf_tnt.ticks", 12); // try to emulate normal

		for (Block block : damagedBlocks) {
			int type_id = block.getTypeId();
			byte data = block.getData();
			
			// Checking for whitelist (list of IDs to destroy) or blacklist (IDs to keep)
			/* if ((ifwhitelist && blockFilter.contains (new BlockId(type_id, data))) || 
				( !ifwhitelist && !blockFilter.contains (new BlockId(type_id, data))) )
			***
			{
				// getDrops is dependent on craftbukkit 1.1 
				Collection<ItemStack> drops = block.getDrops(); 
				// NOTE to self: getDrops does NOT return chest contents. Need to handle chest contents separately.
				
//				plugin.log.finer ("AC: block " + type_id + (ifwhitelist ? "" : " not") + " on " + blockFilter.name()  + "; destroying "+ drops.size() + " stacks");

				if (power <= 0)
					totalDrops += dropItems (drops.toArray(new ItemStack [0]), w, block.getLocation(), yield);
					// could use v=0 with throwItems for same effect, but this is much less processing.
				else 
				{
					int x= loc.getBlockX(), y=loc.getBlockY(), z=loc.getBlockZ();
					Vector v= (block.getLocation().subtract (x,y,z)).toVector();
					double distance = v.length();
					v.normalize().multiply (power/ (distance*distance)); // greater power decreasing by distance squared

					throwItems (drops.toArray(new ItemStack [0]), w, block.getLocation(), v, yield);
				}
				
				// Check for container block, now InventoryHolder, before rewriting it
				BlockState blockState = block.getState();
				if (blockState instanceof InventoryHolder)       {
					InventoryHolder container  = (InventoryHolder)blockState;
					ItemStack[] contents = container.getInventory().getContents();
					
					plugin.log.finer (": dropping container contents of " + contents.length + " stacks");
					dropItems (contents, w, block.getLocation(), 100); // return 100% of what was in chest
					container.getInventory().clear();
				}
				
				//rewrite the block as it was to AIR. This is supposed to destroy it.
				block.setType (Material.AIR);
				block.getState().update(); //force update
			}
		}
		// plugin.log.fine (": filtered drop left " + totalSafe + " alone and dropped " + totalDrops);
	}	
************/
}
