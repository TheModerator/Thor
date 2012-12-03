/* Thor.java
 *  extended from RjSowden by Filbert66, using new ThorPower enum
 * TODO:
 *   - Item names must match configurable string as well as ID.
 * History
 * 28 Nov 2012: Added list, clear commands; added Explode.power config
 *   			Rewrite give to avoid destroying current item in hand, and also add enchant.
 * 29 Nov 2012: Added loadPowerConfig(), .quantity.
 */

package sss.RjSowden.Thor;

import java.util.HashMap;

import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ShapedRecipe;

public class Thor extends JavaPlugin {
	
	static Logger log;	
	static boolean leftclick = false;
	static int wandID = 0;
	static Double xPower = 4D;
	public static HashMap<Player, ThorPower> PlayerMode = new HashMap<Player, ThorPower>();
	public static HashMap<Player, Long>      lastTimes = new HashMap<Player, Long>();
	static ItemStack thorHammer = new ItemStack (wandID, 1);
	
	public void onEnable(){ 
		log = this.getLogger();

		//log.info("THOR is loading...");
		
		this.getServer().getPluginManager().registerEvents(new ThorPlayerListener(this), this);
		
		if(getConfig().getBoolean("DO_NOT_EDIT.READY") == false){
			log.info("No config file found. Creating...");
			getConfig().set("Wand.itemID", Material.STONE_AXE.getId());
			getConfig().set("Wand.leftclick", false);
			getConfig().set("explode.power", xPower);
			getConfig().set("DO_NOT_EDIT.READY", true);
			this.saveDefaultConfig();
			log.info("Done");
		}
		leftclick = getConfig().getBoolean("Wand.leftclick");
		wandID = getConfig().getInt("Wand.itemID");
		thorHammer.setTypeId (wandID);
		thorHammer.addEnchantment (Enchantment.DURABILITY,1);
		// Add thorHammer.setName("Mjollnir");
		
		for (ThorPower p : ThorPower.values()) {
			loadPowerConfig (p.getCommand());
		}
		// Power-specific config items
		getConfig().getInt ("creeper.quantity");	
		log.config ("wolf quantity is " + getConfig().getInt ("wolves.quantity"));		
		xPower = getConfig().getDouble("explode.power");

		addRecipes();
		//log.info("THOR is Loaded");
	}
	
	private void loadPowerConfig (String node) {
		// this doesn't really do anything other than load into RAM
		getConfig().getShortList (node + ".durability");  //casting impact to item durability
		getConfig().getInt (node + ".cooldown");	//required cooldown period in seconds
		getConfig().getInt (node + ".itemID");		// different item ID if desired
		getConfig().getInt (node + ".range");		// how far can this be cast
	}
	
	private void addRecipes () {
		ShapedRecipe hammerRCP = new ShapedRecipe (thorHammer);
		hammerRCP.shape(new String[] { "ABA", " C ", " C " });
		hammerRCP.setIngredient('A', Material.ANVIL);
		hammerRCP.setIngredient('B', Material.NETHER_STAR);
		hammerRCP.setIngredient('C', Material.IRON_INGOT);
		getServer().addRecipe(hammerRCP);
		
		/* Add recipes for lesser-powered (single-powered) items?, using easier core items:
		 *  Teleport - ender eye
		 *  Fire - blaze powder, fire aspect enchant
		 *  Fireball - ghast tear, fire aspect enchant, L2
		 *  explosion - wither skull, 
		 *  napalm - fire charge (made by blaze powder, coal, gunpowder), fire aspect enchant		  
		 *  Alternative shape for "easy" ingredients: shape(new String[] { "ABA", " C ", " C " }):
			 *  Creeper - gunpowder x 5 
			 *  wolf - bonemeal x 5
			 *  lightning - redstone x 5 
		 */
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		String command;
		
		if (args.length < 1 && (sender instanceof Player)){
			sender.sendMessage("### Sowden Software Systems - Bukkit Plugins ###");
			sender.sendMessage("Not enough Arguments");
			return false; 
		}
		else {
			command = (args.length >0) ? args[0].toLowerCase() : "list"; // implied command for console
		}

		if ( !sender.hasPermission ("thor." + command) && 
			!command.equals ("stop") && !command.equals ("help") )
		{
			sender.sendMessage ("Thor: You do not have permission to " + command);
			log.info (sender.getName() + " illegally attemped " + command + " command");
			return true;
		}
		
			/*
			 * Console-allowable commands
			 */
			if (command.equals("clear")) {
				PlayerMode.clear();
				return true;
			}			
			if (command.equals("list"))  
			{ 
				sender.sendMessage ("Players currently with Thor powers:");
				for (Player p : Thor.PlayerMode.keySet()) {
					sender.sendMessage (p.getName() + ": " + Thor.PlayerMode.get(p).getCommand());
				}
				sender.sendMessage ("Players who have used Thor powers:");
				long currTime = System.currentTimeMillis();
				for (Player p : Thor.lastTimes.keySet()) {
					double minutesAgo = (currTime - Thor.lastTimes.get(p))/1000/60.0D;
					sender.sendMessage (p.getName() + ": " + minutesAgo + " minutes ago");
				}
				return true;				
			}
			if (command.equals("help")){
				sender.sendMessage(ChatColor.YELLOW + "### Sowden Software Systems - Bukkit Plugins ###");
				sender.sendMessage(ChatColor.YELLOW + " You asked for help?");
				sender.sendMessage(ChatColor.YELLOW + "   Actions in your version are:");
				sender.sendMessage(ChatColor.YELLOW + "   " + ThorPower.commandList + "list, stop, clear, help, give");
				return true; 
			}	
		
			if ( !(sender instanceof Player)) {
				sender.sendMessage ("Thor " + command + " not a valid option from console");
				return true;
			}		
			// else we know we have a Player
			Player player = (Player)sender;
			
			if (command.equals("stop")){
				if (PlayerMode.containsKey (player)) {
					PlayerMode.remove (player);
					sender.sendMessage(ChatColor.RED + "Thor Powers Stopped");
				} else {
					sender.sendMessage ("You have no Thor powers");
				}

				return true; 
			}
			for (ThorPower power : ThorPower.values()) {
				if (command.equals (power.getCommand())) {
					PlayerMode.put(player,power);
					sender.sendMessage (ChatColor.UNDERLINE + (leftclick ? "Left" : "Right") + ChatColor.RESET + " click to " + power.getPhrase());
					return true;
				}
			}

			if (command.equals("give")){
				ItemStack item = player.getItemInHand(); // can be empty
				if (item != null) { // try to store in inventory
					int slot = player.getInventory().firstEmpty();
					if (slot != -1)
						player.getInventory().setItem (slot, item);
					else {
						sender.sendMessage ("Unable to store " + item.getType() + " from hand. " + ChatColor.RED + "Inventory full");
						return true;
					}
				}
				
				// Now give item
				sender.sendMessage(ChatColor.YELLOW + "Here is Mjollnir. Use responsibly!");
				// Be nice to give it a name "Mjollnir", but what's the API?
				player.setItemInHand (thorHammer);
				sender.sendMessage("use a " + ChatColor.UNDERLINE + (leftclick ? "Left" : "Right") + ChatColor.RESET + " CLICK, as specified in the config");
				return true; 
			}	
			sender.sendMessage(ChatColor.YELLOW + "### Sowden Software Systems - Bukkit Plugins ###");
			sender.sendMessage(ChatColor.YELLOW + "   Unknown arguement:" + command);
			
			return false;
	}
		
	public void onDisable(){ 
		PlayerMode.clear();
		lastTimes.clear();
		
		log.info("THOR disabled");
	}
}
