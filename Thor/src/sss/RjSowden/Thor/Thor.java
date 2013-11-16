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
 * 01 Nov 2013 : Added configurable item meta. 
 */

package sss.RjSowden.Thor;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Skull;
import org.bukkit.SkullType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;
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
	private ItemStack[] powerTool = new ItemStack [ThorPower.count];
	private Recipe[] powerToolRecipe = new Recipe [ThorPower.count];
	
	public void onEnable(){ 
		boolean writeDefault = false;
		
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

		if ( !getDataFolder().exists() || !(new File (getDataFolder(), "config.yml").exists())) {
			// Don't write defaults all the time as it erases comments in the file
			writeDefault = true;
			getConfig().options().copyDefaults(true);
			log.info ("No config found in " + getDescription().getName() + "/; writing defaults");
		}
		
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
			
		if (writeDefault)
			saveDefaultConfig();
			
		//log.info("THOR is Loaded");
	}
	
    private static final HashMap<String, Material> altNames;
    static
    {
        altNames = new HashMap<String, Material>();
        altNames.put("GUNPOWDER", Material.SULPHUR);
        altNames.put("WOOD_SHOVEL", Material.WOOD_SPADE);
        altNames.put("STONE_SHOVEL", Material.STONE_SPADE);
        altNames.put("IRON_SHOVEL", Material.IRON_SPADE);
        altNames.put("GOLD_SHOVEL", Material.GOLD_SPADE);
        altNames.put("DIAMOND_SHOVEL", Material.DIAMOND_SPADE); 
        altNames.put("FIRECHARGE", Material.FIREBALL);
        altNames.put("ENDER_EYE", Material.EYE_OF_ENDER);             

		// Must call matchMaterialData for these        
        altNames.put("WITHER_SKULL", Material.SKULL_ITEM);
        altNames.put("CREEPER_HEAD", Material.SKULL_ITEM);
        altNames.put("SKELETON_SKULL", Material.SKULL_ITEM);
        altNames.put("PLAYER_HEAD", Material.SKULL_ITEM);
        altNames.put("ZOMBIE_HEAD", Material.SKULL_ITEM);
    }
	private Material matchMaterial (String name) {
		Material mat = Material.matchMaterial (name);
		if (mat == null) {
			name = name.toUpperCase();
			
			mat = altNames.get (name);
			if (mat != null) {
				log.config ("Matched alternative name '" + name + "' for " + mat);
			}
		}
		return mat;
	}
	private MaterialData matchMaterialData (Material mat, String name) {
		if (mat == Material.SKULL_ITEM) {
			SkullType st = SkullType.SKELETON; // zero
			try {
				st = SkullType.valueOf (name.substring (0,name.indexOf ('_')));
			} catch (Exception e) {
				log.warning ("unrecognized skull type: '" + name + "'");
			}
			Skull skullData = new Skull (Material.SKULL_ITEM, (byte)st.ordinal());
			return skullData;
		}
		
		return null;
	}

	
	// Attempts to match config string to a Material type. 
	//   If not, tries to match to <configstring>+"ID" using deprecated calls
	//   Returns: configured Material or default; can be null
	private Material getItemType (String configPath) {
		Material mat = null;
		
		if (getConfig().isSet (configPath)) {
			mat = matchMaterial (getConfig().getString (configPath));
			if (mat == null) {
				log.warning (configPath + ": unknown Material string '" + getConfig().getString (configPath) + "'. Refer to http://bit.ly/1hjNiY7");
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
		if (item == null || powerTool [power.ordinal()] == null ||
			item.getType() != powerTool [power.ordinal()].getType())
			return false;

		ItemMeta meta = powerTool [power.ordinal()].getItemMeta(); 
		if (meta != null) {
			ItemMeta testMeta = item.getItemMeta();
			
			if (meta.hasDisplayName() && 
				(!testMeta.hasDisplayName() || ! meta.getDisplayName().equals (testMeta.getDisplayName())) )
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
	// Returns Item of provided Power from Configuration data.
	//  if no item configured, returns null
	private ItemStack loadWand (ThorPower power) {
		Material itemType = getItemType (power.getCommand() + ".item");
		if (itemType == null)
			return null;
		ItemStack item = new ItemStack (itemType);
		
		ConfigurationSection metaConfig = getConfig().getConfigurationSection (power.getCommand() + ".meta");
		if (metaConfig == null)
			return item;
		ItemMeta meta = item.getItemMeta();  // get empty copy of an ItemMeta
		meta.setDisplayName (metaConfig.getString ("name"));
		meta.setLore (metaConfig.getStringList ("lore"));
		
		if (metaConfig.isConfigurationSection ("enchants")) 
		{
			for (String enchantString : metaConfig.getConfigurationSection ("enchants").getKeys (false /*depth*/)) {
				Enchantment enchant = Enchantment.getByName (enchantString);
				if (enchant == null) {
					log.warning ("Unknown enchantment: " + enchantString + ". Refer to http://bit.ly/HxVS58");
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
		item.setItemMeta (meta);
		return item;
	}
	// Allow for shaped or shapeless (quantities of material)
	private Recipe loadRecipe (ThorPower power) {
		ConfigurationSection rcpCfg = getConfig().getConfigurationSection (power.getCommand() + ".recipe");
		ItemStack tool = powerTool [power.ordinal()];
		Recipe r = null;
		String powerName = power.getCommand();
		
		if (rcpCfg == null || tool == null)
			return null;
		
		// Build shaped recipe
		if (rcpCfg.isSet ("shape")) 
		{
			ShapedRecipe sr = new ShapedRecipe (tool);
			/** Format: 
			 * ingredients:
			 *   DIRT: A
			 *   REDSTONE: B
			 *   DIAMOND: C
			 * shape:
			 *   - "AAA"
			 *   - " C "
			 *   - "BBB"
			 */
			// Must load shape first, since setIngredient checks against it.
			List<String> format = getConfig().getStringList (power.getCommand() + ".recipe.shape");
			if (format.size() != 3) {
				log.warning (powerName + ".recipe.shape must be 3 strings of 3 characters");
				return null;
			}
			for (String f : format) {
				if (f.length() != 3) {
					log.warning (powerName + ".recipe.shape must be 3 strings of 3 characters");
					return null;
				}
			}
			sr.shape(format.toArray(new String[3]));
			 			
			// Get ingredients
			if ( !rcpCfg.isSet ("ingredients")) {
				log.warning (powerName + ".recipe.shape.ingredients not set");
				return null;
			}
			for (String matString : rcpCfg.getConfigurationSection ("ingredients").getKeys(/*deep=*/false)) {
				Material mat = matchMaterial (matString);
				MaterialData data = null;
				
				if (mat == null) {
					log.warning (powerName + ".recipe.shape.ingredients: '" + matString + "' unrecognized Material. Refer to http://bit.ly/1hjNiY7");
					continue;
				}
				data = matchMaterialData (mat, matString);
				
				String letter = rcpCfg.getConfigurationSection ("ingredients").getString (matString);
				if (letter.length() != 1) {
					log.warning (powerName + ".recipe.shape.ingredients." + matString + " unrecognized letter '" + letter + "'");
					continue;
				}
				// Check that each char is in the shape; if not, it's OK; we can build
				String[] shape= sr.getShape();
				String fullRCP = shape[0].concat (shape[1]).concat (shape [2]);
				if ( !(fullRCP.indexOf (letter) != -1)) {
						log.warning (powerName + ".recipe.ingredients contains symbol '" + letter + "' not in .shape");
				}				

				if (data != null) {
					sr.setIngredient (letter.charAt(0), data);
					log.config ("Recognized special material: " + data);
				} else
					sr.setIngredient (letter.charAt(0), mat);
			}
			// Check that each char in Shape is listed as an ingredient
			for (String row : format) 
				for (char c : row.toCharArray()) 
					if (c != ' ' && sr.getIngredientMap ().get (c) == null) {
						log.warning (powerName + ".recipe.shape contains unlisted ingredient '" + c + "'");
						return null;  // can't build it
					}
			r = sr;
		}
		else if (rcpCfg.isSet ("quantities")) {
			ShapelessRecipe slr = new ShapelessRecipe (tool);
			MaterialData data = null;

			/** Format: 
			 * quantities:
			 *   DIRT: 2
			 *   REDSTONE: 1
			 *   DIAMOND: 2
			 */
			int totalAmount = 0;
			for (String matString : rcpCfg.getConfigurationSection ("quantities").getKeys(/*deep=*/false)) {
				Material mat = matchMaterial (matString);
				if (mat == null) {
					log.warning (powerName + ".recipe.quantities: '" + matString + "' unrecognized Material. Refer to http://bit.ly/1hjNiY7");
					continue;
				}
				data = matchMaterialData (mat, matString);

				int amount = rcpCfg.getConfigurationSection ("quantities").getInt (matString);
				totalAmount += amount;
				if (amount < 1 || amount > 9) {
					log.warning (powerName + ".recipe.quantities." + matString + " unexpected amount '" + amount + "'");
					continue;
				}
				else if (totalAmount > 9) {
					log.warning (powerName + ".recipe.quantities: cannot have more than 9 ingredients");
					continue;
				}
				if (data != null) {
					slr.addIngredient (amount, data);
					log.config ("Recognized special material: " + data);
				} else
					slr.addIngredient (amount, mat);
			}
			
			r= slr;
		} else {
			log.warning (powerName + ".recipe must include either 'shape' or 'quantities' node");
	 	}
	 			
	 	return r;
	}

	private void loadPowerConfig (String node) {
		ThorPower p = ThorPower.getPower (node);

		powerTool[p.ordinal()] = loadWand (p);	// to get any errors on startup	
		powerToolRecipe [p.ordinal()] = loadRecipe(p);
		
		// rest doesn't really do anything
		getConfig().getShortList (node + ".durability");  //casting impact to item durability
		getConfig().getInt (node + ".cooldown");	//required cooldown period in seconds
		getConfig().getInt (node + ".range");		// how far can this be cast
	}
	
	private void addRecipes () {
		ShapedRecipe hammerRCP = new ShapedRecipe (thorHammer);
		hammerRCP.shape(new String[] { "ABA", " C ", " C " });
		hammerRCP.setIngredient('A', Material.ANVIL);
		hammerRCP.setIngredient('B', Material.NETHER_STAR);
		hammerRCP.setIngredient('C', Material.IRON_INGOT);
		getServer().addRecipe(hammerRCP);
		
		for (int i=0; i<powerToolRecipe.length; i++) {
			Recipe r = powerToolRecipe[i];
			if (r != null) {
				getServer().addRecipe (r);
				log.config ("Added recipe for " + ThorPower.getPower (i+1).getCommand() + " tool (" + 
							(powerTool[i].getItemMeta().hasDisplayName() ? 
							powerTool[i].getItemMeta().getDisplayName() : powerTool[i].getType() ) + ")" );
			}
		}
	     //  Future: create new items with custom image, ID, etc. Would have to be packaged with TexturePack.
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
