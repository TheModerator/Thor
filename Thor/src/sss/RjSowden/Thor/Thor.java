/* Thor.java
 *  extended from RjSowden by Filbert66, using new ThorPower enum
 * TODO:
 * History
 * 28 Nov 2012: Added list, clear commands; added Explode.power config
 *   			Rewrite give to avoid destroying current item in hand, and also add enchant.
 * 29 Nov 2012: Added loadPowerConfig(), .quantity.
 * 24 Oct 2013: Began removing deprecated Bukkit calls, use "item" before "itemID".
 *              Added cooldownEnabled global
 *              Added name and lore (i.e. instructions) to thorHammer, making dep on Bukkit 1.4.5 
 *              Integrated changes from "1.75" (3.1) from RjSowden.
 */

package sss.RjSowden.Thor;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ShapedRecipe;

public class Thor extends JavaPlugin {
	
	static Logger log;	
	static String Log_Level;
	static boolean leftclick = false;
	static Material wandID = Material.AIR;
	static Double xPower = 4D;
	public static HashMap<Player, ThorPower> PlayerMode = new HashMap<Player, ThorPower>();
	public static HashMap<Player, Long>      lastTimes = new HashMap<Player, Long>();
	static ItemStack thorHammer = new ItemStack (wandID, 1);
	static boolean durabilityEnabled;
	public static boolean cooldownEnabled;
	public static boolean targetingEnabled;
	
	public void onEnable(){ 
		log = this.getLogger();
		if (getConfig().isString ("log_level")) {
			Log_Level = getConfig().getString("log_level", "INFO"); // hidden config item
			try {
				log.setLevel (log.getLevel().parse (Log_Level));
				log.info ("successfully set log level to " + log.getLevel());
			}
			catch (Throwable IllegalArgumentException) {
				log.warning ("Illegal log_level string argument '" + Log_Level);
			}
		} else 
			log.setLevel (Level.INFO);

		this.reloadConfig();  // load any confg
		this.getConfig();	  // get config to RAM, if any
		getConfig().options().copyDefaults(true);
		this.saveConfig();     // write config + defaults to disk
		this.reloadConfig();
		
		//log.info("THOR is loading...");
		
		this.getServer().getPluginManager().registerEvents(new ThorPlayerListener(this), this);
		
		leftclick = getConfig().getBoolean("Hammer.leftclick");
		wandID = getItemType("Hammer.item");

		thorHammer.setType (wandID);
		thorHammer.addEnchantment (Enchantment.DURABILITY,1);
		// Load meta for thorHammer
		ItemMeta meta = thorHammer.getItemMeta();
		meta.setDisplayName("Mjollnir");
		String[] lore = {"also known as Thor's hammer",  "Mjollnir can cast many spells", "by right-clicking.",
								"To set which spell to cast", "use /thor command"};
		meta.setLore (Arrays.asList (lore));
		thorHammer.setItemMeta (meta);
		
		for (ThorPower p : ThorPower.values()) {
			loadPowerConfig (p.getCommand());
		}
		// Power-specific config items
		getConfig().getInt ("creeper.quantity");	

		xPower = getConfig().getDouble("explode.power");
		durabilityEnabled = getConfig().getBoolean("Generic.enableDurability");
		cooldownEnabled = getConfig().getBoolean("Generic.enableCooldown");
		targetingEnabled = getConfig().getBoolean("Generic.enableTargeting");
		if (getConfig().getBoolean ("Generic.enableCrafting"))
			addRecipes();
		//log.info("THOR is Loaded");
	}
	
	// Attempts to match config string to a Material type. 
	//   If not, tries to match to <configstring>+"ID" using deprecated calls
	//   Returns: configured Material or default; can be null
	private Material getItemType (String configPath) {
		Material mat = null;
		
		if (getConfig().isSet (configPath)) {
			mat = Material.matchMaterial (getConfig().getString (configPath));
			if (mat == null) {
				log.warning (configPath + ": unknown Material string '" + getConfig().getString (configPath));
			}
		}
		// Else try the old ID form
		if (mat == null) {
			int itemID = getConfig().getInt(configPath + "ID", 0);
			if (itemID != 0) {
				mat = Material.getMaterial (itemID);
				if (mat == null) {
					log.warning (configPath + "ID: Unknown item ID: " + itemID);
				} 
			}
		}
		// else get the default string, if it exists
		if (mat == null) 
			mat = Material.getMaterial (getConfig().getDefaults().getString(configPath));
		return mat;
	}
	
	// returns true if matches required characteristics of thorHammer
	// Must match ID, enchant, name, have lore
	boolean matchesHammer (ItemStack item) {
		ItemMeta meta = item.getItemMeta();

		if (item.getType() == thorHammer.getType() &&
			item.containsEnchantment (Enchantment.DURABILITY) &&
			thorHammer.getItemMeta().getDisplayName().equals (meta.getDisplayName()) &&
			meta.hasLore() )
			return true;
		else
			return false;
	}
	// returns true if item matches required characteristics of item for that power
	// Must match ID, enchant, name, have lore
	boolean matchesPowerWand (ThorPower power, ItemStack item) {
		if (item.getType() != getItemType (power.getCommand() + ".item"))
			return false;

		ItemMeta meta = loadWandMeta (power); 
		if (meta != null) {
			ItemMeta testMeta = item.getItemMeta();
			
			if (! meta.getDisplayName().equals (testMeta.getDisplayName()))
				return false;
			if (meta.hasLore() && !testMeta.hasLore())
				return false;
			if (meta.hasEnchants()) {
				for (Enchantment enchant : meta.getEnchants().keySet()) {  
					// match if enchant on provided wand is greater or equal
					if (meta.getEnchantLevel (enchant) > testMeta.getEnchantLevel (enchant))
						return false;
				}
			}
		}
		return true;
	}
	// Returns ItemMeta of provided Power from Configuration data.
	//  Maybe should pre-load into an array so that don't have to build each time?
	//  Pre-load ItemStack or ItemMeta? Easily could be itemStack 
	private ItemMeta loadWandMeta (ThorPower power) {
		ItemStack item = new ItemStack (getItemType (power.getCommand() + ".item"));
		ItemMeta meta = item.getItemMeta();  // get empty copy of an ItemMeta
		
		ConfigurationSection metaConfig = getConfig().getConfigurationSection (power.getCommand() + ".meta");
		if (metaConfig == null)
			return null;
		meta.setDisplayName (metaConfig.getString ("name"));
		meta.setLore (metaConfig.getStringList ("lore"));
		
		if (metaConfig.isConfigurationSection ("enchants")) 
		{
			for (String enchantString : metaConfig.getConfigurationSection ("enchants").getKeys (false /*depth*/)) {
				Enchantment enchant = Enchantment.getByName (enchantString);
				if (enchant == null) {
					log.warning ("Unknown enchantment: " + enchantString + ". See http://jd.bukkit.org/rb/apidocs/org/bukkit/enchantments/Enchantment.html");
				} else {
					int level = metaConfig.getInt ("enchants." + enchantString);
					if (level < 1) {
						log.warning ("Unsupported " + enchantString + " enchant level: " + level);
					} else { 
						meta.addEnchant (enchant, level, /* ignoreRestrictions= */false);
					}
				}
			}
		}
		return meta;
	}

	private void loadPowerConfig (String node) {
		// this doesn't really do anything other than load into RAM
		getConfig().getShortList (node + ".durability");  //casting impact to item durability
		getConfig().getInt (node + ".cooldown");	//required cooldown period in seconds
		getItemType (node + ".item");		
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
	     * Allow config to set not only item ID, but name, lore.
	     *  Then to protect cheap manufacture of given item, only allow power when item has right name, enchant, (& lore?). 
	     *  Future: create new items with custom image, ID, etc. Would have to be packaged with TexturePack.
		 */
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		String command;
		
		if (args.length < 1 && (sender instanceof Player)){
			// sender.sendMessage(ChatColor.RED + "### ROEStudios - Bukkit Plugins ###");
			sender.sendMessage(ChatColor.RED + "More Information Required!");
			return false; 
		}
		else {
			command = (args.length >0) ? args[0].toLowerCase() : "list"; // implied command for console
		}

		if ( !sender.hasPermission ("thor." + command) && 
			!command.equals ("stop") && !command.equals ("help") )
		{
			sender.sendMessage ("Thor: You do not have permission to " + command);
			getServer().getConsoleSender().sendMessage ("[THOR ILLEGAL USAGE REPORT] " +sender.getName() + " illegally attemped " + command + " command");
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
//				sender.sendMessage(ChatColor.RED + "### ROEStudios - Bukkit Plugins ###");
				sender.sendMessage(ChatColor.YELLOW + " You asked for help?");
				sender.sendMessage(ChatColor.YELLOW + "   Actions in your version are:");
				sender.sendMessage(ChatColor.YELLOW + "   " + ThorPower.commandList + "list, stop, clear, help, give");
				return true; 
			}	
		
			// PLayer-allowable commands
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
					sender.sendMessage (ChatColor.UNDERLINE + (leftclick ? "<< Left" : "Right >>") + ChatColor.RESET + " click to " + power.getPhrase());
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
				sender.sendMessage(ChatColor.YELLOW + "Here is " +ChatColor.LIGHT_PURPLE + thorHammer.getItemMeta().getDisplayName() + ChatColor.YELLOW + ". Use responsibly!");
				player.setItemInHand (thorHammer);
				sender.sendMessage("use a " + ChatColor.UNDERLINE + (leftclick ? "<< Left" : "Right >>") + ChatColor.RESET + " CLICK, as specified in the config");
				return true; 
			}	
//			sender.sendMessage(ChatColor.YELLOW + "### ROEStudios - Bukkit Plugins ###");
			sender.sendMessage(ChatColor.YELLOW + "Unknown arguement: " + command);
			
			return false;
	}
		
	public void onDisable(){ 
		PlayerMode.clear();
		lastTimes.clear();
		
		log.info("THOR disabled");
	}
}
