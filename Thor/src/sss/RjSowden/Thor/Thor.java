package sss.RjSowden.Thor;

import java.util.HashMap;

import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class Thor extends JavaPlugin {
	
	Logger log = Logger.getLogger("Minecraft");
	
	static boolean leftclick = false;
	static int wandID = 0;
	public static HashMap<Player, Integer> PlayerMode = new HashMap<Player, Integer>();
	
	public void onEnable(){ 
		log.info("THOR is loading...");
		
		PluginManager pm = this.getServer().getPluginManager();
		getServer().getPluginManager().registerEvents(new ThorPlayerListener(), this);
		
		if(getConfig().getBoolean("DO_NOT_EDIT.READY") == false){
			log.info("[Thor]: No config file found. Creating...");
		getConfig().set("Wand.itemID", 275);
		getConfig().set("Wand.leftclick", false);
		getConfig().set("DO_NOT_EDIT.READY", true);
		this.saveConfig();
		getConfig().set("DO_NOT_EDIT.READY", true);
		getConfig().set("Wand.itemID", 275);
		getConfig().set("Wand.leftclick", false);
		this.saveConfig();
		log.info("[Thor]: Done");
		this.reloadConfig();
		}
		leftclick = getConfig().getBoolean("Wand.leftclick");
		wandID = getConfig().getInt("Wand.itemID");
		log.info("THOR is Loaded");
	}
	 
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		
			
			if (args.length < 1){
				sender.sendMessage("�e### Sowden Software Systems - Bukkit Plugins ###");
				sender.sendMessage("�eERROR found whilst using THOR");
				sender.sendMessage("�eNot enough Arguments");
				sender.sendMessage("�eUse /Thor [power]");
				return false; 
			}
			
			if (args[0].equalsIgnoreCase("stop")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),0);
				sender.sendMessage("�2Thor Powers Stopped");
				return true; 
			}
			if (args[0].equalsIgnoreCase("lightning")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),1);
				if (leftclick == true){
				sender.sendMessage("�2Left click to use send lightning");
				} else {
					sender.sendMessage("�2Right click to use send lightning");
				}
				return true; 
			}		
			if (args[0].equalsIgnoreCase("explode")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),2);
				if (leftclick == true){
					sender.sendMessage("�2Left click to cause explosion");
					} else {
						sender.sendMessage("�2Right click to cause explosion");
					}
				return true; 
			}	
			if (args[0].equalsIgnoreCase("wolves")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),3);
				if (leftclick == true){
					sender.sendMessage("�2Left click to spawn wolves");
					} else {
						sender.sendMessage("�2Right click to spawn wolves");
					}
				return true; 
			}	
			if (args[0].equalsIgnoreCase("teleport")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),4);
				if (leftclick == true){
					sender.sendMessage("�2Left click location to teleport to");
					} else {
						sender.sendMessage("�2Right click location to teleport to");
					}
				return true; 
			}	
			if (args[0].equalsIgnoreCase("fireball")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),5);
				if (leftclick == true){
					sender.sendMessage("�2Left click to throw a fireball");
					} else {
						sender.sendMessage("�2Right click to throw a fireball");
					}
				return true; 
			}	
			if (args[0].equalsIgnoreCase("fire")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),6);
				if (leftclick == true){
					sender.sendMessage("�2Left click what you want to set on fire");
					} else {
						sender.sendMessage("�2Right click what you want to set on fire");
					}
				return true; 
			}
			if (args[0].equalsIgnoreCase("fire")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),6);
				if (leftclick == true){
					sender.sendMessage("�2Left click what you want to set on fire");
					} else {
						sender.sendMessage("�2Right click what you want to set on fire");
					}
				return true; 
			}	
			if (args[0].equalsIgnoreCase("obliterate")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),7);
				if (leftclick == true){
					sender.sendMessage("�2Left click the area you want to obliterate");
					} else {
						sender.sendMessage("�2Right click the area you want to obliterate");
					}
				return true; 
			}
			if (args[0].equalsIgnoreCase("del") | args[0].equalsIgnoreCase("delete")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),8);
				if (leftclick == true){
					sender.sendMessage("�2Left click what you want to set on fire");
					} else {
						sender.sendMessage("�2Right click what you want to set on fire");
					}
				return true; 
			}	
			if (args[0].equalsIgnoreCase("power")){
				PlayerMode.put(getServer().getPlayer(sender.getName()),9);
				if (leftclick == true){
					sender.sendMessage("�2Left click the block you want to toggle redstone power");
					} else {
						sender.sendMessage("�2Right click block area you want to toggle redstone power");
					}
				return true; 
			}
			if (args[0].equalsIgnoreCase("help")){
				sender.sendMessage("### Sowden Software Systems - Bukkit Plugins ###");
				sender.sendMessage(" You asked for help?");
				sender.sendMessage("   Actions in your version are");
				sender.sendMessage("   Stop, Lightning, Explode, Wolves, Teleport, Fireball, Fire, Obliterate, Del/Delete, Power");
				return true; 
			}	
			if (args[0].equalsIgnoreCase("give")){
				sender.sendMessage("   Here is the hammer you can use to wreak havoc with!");
				Player master = (Player) sender;
				ItemStack item = master.getItemInHand();
				item.setTypeId(wandID);
				item.setAmount(1);
				master.setItemInHand(item);
				if (leftclick == true){
					sender.sendMessage("use a LEFT CLICK, as specified in the config");
					} else {
						sender.sendMessage("use a RIGHT CLICK, as specified in the config");
					}
				return true; 
			}	
			sender.sendMessage("### Sowden Software Systems - Bukkit Plugins ###");
			sender.sendMessage(" ERROR found whilst using THOR");
			sender.sendMessage("   Unknown arguement. Current known arguments are:");
			sender.sendMessage("   Stop, Lightning, Explode, Wolves, Teleport, Fireball, Fire, Obliterate, Del/Delete, Power");
			
			
			return false;
			
		
	}
	
	
	
	
	
	
	public void onDisable(){ 
		
		
		log.info("THOR disabled");
	}
}
