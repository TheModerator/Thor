/* ThorPlayerListener.java
 *  extended from RjSowden by Filbert66, using new ThorPower enum
 * TODO:
 * History
 * 28 Nov 2012: Added CREEPER, SMALLFIREBALL powers, rewrote FIRE and FIREBALL
 * 02 Nov 2012: Added DELETE, POWER. Collapsed WOLF & CREEPER.
 */

package sss.RjSowden.Thor;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.ChatColor;

//import org.bukkit.plugin.java.JavaPlugin;
//import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Random;

import sss.RjSowden.Thor.ThorPower;


public class ThorPlayerListener implements Listener{
	private final Thor plugin;
	
	public ThorPlayerListener (Thor instance)
	{
		this.plugin = instance;
	}

	@EventHandler (ignoreCancelled = false)
	public void onPlayerInteract(PlayerInteractEvent event){
		// Because you have to click AIR, cannot be within range of "touch". This is a good built-in 
		//  safety feature for dangerous actions.
		
		if ((event.getAction().equals(Action.LEFT_CLICK_AIR) & Thor.leftclick == true ) | 
			(event.getAction().equals(Action.RIGHT_CLICK_AIR) & Thor.leftclick == false ) ) {
			Player player = event.getPlayer();
			int itemInHand = player.getItemInHand().getTypeId();
			
			if (itemInHand == Thor.wandID) {
			  if (Thor.PlayerMode.containsKey(player) == true)
				usePower (player, Thor.PlayerMode.get(player));
			}	
			// Check new <power>.itemID list and if present ignore PlayerMode
			// FUTURE: check that item is named properly, on top of ID.
			else for (ThorPower power : ThorPower.values()) {
				int powerItemID = plugin.getConfig().getInt (power.getCommand() + ".itemID");
				
				if (powerItemID !=0 && powerItemID == itemInHand) {
					usePower (player, power);
					break;  // otherwise one cast could have multiple effects
				}
			}
		}
	}
	
	// Below function could be moved to ThorPower completely, removing "mode" param, but is it worth it?
	void usePower (Player player, ThorPower mode) {
		Location loc;
		int distance = plugin.getConfig().getInt (mode.getCommand() + ".distance");
		Random rand = new Random(); //randomize locations so mobs don't mirror one another.

		// Check cooldown
		long cooldown = 1000 * plugin.getConfig().getInt (mode.getCommand() + ".cooldown");
		if (cooldown > 0) {
			Long lastTime = Thor.lastTimes.get (player);
			long currTime = System.currentTimeMillis();
			if (lastTime != null && lastTime.longValue() + cooldown >= currTime) {
				player.sendMessage ("Thor: You need to wait " + (cooldown - (currTime - lastTime))/1000 + " seconds before casting again");
				return;
			}
		}	// write below all the time just so we can keep track of players using Thor.
		Thor.lastTimes.put (player, Long.valueOf(System.currentTimeMillis()));					
		
		// Impact Durability
		short durabilityImpact = (short) plugin.getConfig().getInt (mode.getCommand() + ".durability");
		// Should always get one based on having default values
		if (durabilityImpact > 0) {
			loc = player.getLocation();
			
			int finalD = player.getItemInHand().getDurability() + durabilityImpact;
			if (finalD >= player.getItemInHand().getType().getMaxDurability())
			{  // for now, use durability of underlying item. Could make configurable for Thor's hammer
			   Location eye = player.getEyeLocation();
			   
			   player.setItemInHand(null);
			   loc.getWorld().playEffect (eye, Effect.SMOKE, 0);
			   loc.getWorld().playSound (loc, Sound.ITEM_BREAK, 5F, 2F/*pitch*/);
			   loc.getWorld().playSound (loc, Sound.AMBIENCE_THUNDER, 1F, 0.5F/*pitch*/);
			   player.sendMessage ("Amazingly, Mjollnir disappears with a puff of smoke");
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
				player.getWorld().createExplosion(loc, Thor.xPower.floatValue());
				break;
	
			case WOLF: 
			case CREEPER:
				loc = player.getTargetBlock(null, distance).getLocation();
				loc.setY(loc.getY() + 2);
				for (int i = 0; i < plugin.getConfig().getInt(mode.getCommand() + ".quantity"); i++) {
					Location rLoc = loc;
					rLoc.setX(loc.getX() + (rand.nextFloat()*i - i/2));
					rLoc.setZ(loc.getZ() + (rand.nextFloat()*i - i/2));
					/* Idea: new command "spawn" which requires a creature param. Would have to store that 
					 *  creature type (converting Entity(String) match) in a global hashmap
					 * Then if "spawn" mode = <stored EntityClass>;
					 */
					switch (mode) {
					   case WOLF:
							Wolf w = (Wolf) player.getWorld().spawn (rLoc, Wolf.class);	
							w.setAngry(true);
							break;
						case CREEPER:
							Creeper c = (Creeper) player.getWorld().spawn (rLoc, Creeper.class);	
							break;
						default:
							plugin.log.warning ("unexpected creature type" + mode);
							break;
					}
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
					Thor.log.warning ("Unexpected size (" + targets.size() + ") from getLastTwoTargetBlocks");
					return;
				}
				Block TB = targets.get (1); // last item is target block
				Block before = targets.get (0);
				
				//if target is burnable and before is AIR
				if ( !TB.isEmpty() && !TB.isLiquid() && before.getTypeId() == 0)
					before.setType (Material.FIRE);
				
				// Should try to burn an Entity if that is targeted, but that's difficult, and SMALLFIREBALL 
				//  does the trick, so not worth fixing. 
				return;     
				
			case DELETE:
				loc = player.getTargetBlock(null, distance).getLocation();
				loc.getBlock().setType (Material.AIR);
			    loc.getWorld().playEffect (loc, Effect.SMOKE, 0);
			    loc.getWorld().playSound (loc, Sound.CHICKEN_EGG_POP, 3F/*vol*/, 2F/*pitch*/);
				break;
				
			case POWER:
				loc = player.getTargetBlock(null, distance).getLocation();
				Block b = loc.getBlock();
				Block redstone;
				if (b.getBlockPower() == 0){					
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
					b.setData((byte) 0xF, true);
					player.sendMessage("Power switched " + ChatColor.GREEN + "ON");
				}else{
					if(b.getRelative(BlockFace.NORTH).getType() == Material.REDSTONE_WIRE ){
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
					b.setData((byte) 0x0, true);
					player.sendMessage("Power switched " + ChatColor.RED + "OFF");
				}
				break;
				
		}	
	}
}
