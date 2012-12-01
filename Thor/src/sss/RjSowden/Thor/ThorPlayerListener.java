package sss.RjSowden.Thor;
import net.minecraft.server.Material;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;




public class ThorPlayerListener implements Listener{

	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event){
		if ((event.getAction().equals(Action.LEFT_CLICK_AIR) & Thor.leftclick == true ) | (event.getAction().equals(Action.RIGHT_CLICK_AIR) & Thor.leftclick == false )) {
			Player player = event.getPlayer();
			if (player.getItemInHand().getTypeId() == Thor.wandID) {
		
		//do whatever you want to happen when a player moves here
		//0 = no action
		//1 = lightning
		//2 = explosion
		//3 = spawn angry wolves
		//4 = Teleport player
				if (Thor.PlayerMode.containsKey(player) == true){
					Location blockloc = player.getTargetBlock(null, 256).getLocation();
		switch (Thor.PlayerMode.get(player)){
		case 0:
			return;
		case 1: // Fire Lightning
			Block TB = player.getTargetBlock(null, 256);
			Location LC = TB.getLocation();
			player.getWorld().strikeLightning(LC);
			return;
		case 2:						
			Location LC1 = player.getTargetBlock(null, 256).getLocation();
			player.getWorld().createExplosion(LC1, 9F);
			return;
		case 3:						
			Location LC2 = player.getTargetBlock(null, 256).getLocation();
			LC2.setY(LC2.getY() + 2);
			Wolf w = (Wolf) player.getWorld().spawnCreature(LC2, EntityType.WOLF);	
			w.setAngry(true);
			return;
		case 4:						
			Location LC3 = player.getTargetBlock(null, 256).getLocation();
			LC3.add(0, 3, 0);
			player.teleport(LC3);
			return;
		case 5:						
			Location loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
			
			Fireball fireball = player.getWorld().spawn(loc, Fireball.class);
			fireball.setShooter(player);
			fireball.setIsIncendiary(true);
			fireball.setFireTicks(9999);
			return;
		case 6:						
			Location locat = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
			locat.getBlock().setTypeId(51);
			player.getWorld().getBlockAt(locat).setTypeId(51);
			
			return;       
		case 7:						
			Location LC4 = player.getTargetBlock(null, 256).getLocation();
			player.getWorld().createExplosion(LC4, 9F);
			return;	
		case 8:						
			blockloc.getBlock().setTypeId(0);
			return;
		case 9:						
			Block b = blockloc.getBlock();
			Block redstone;
			if (b.getBlockPower() == 0){
				
				if(b.getRelative(BlockFace.NORTH).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.NORTH);
				}else if(b.getRelative(BlockFace.SOUTH).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.SOUTH);
				}else if(b.getRelative(BlockFace.EAST).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.EAST);
				}else if(b.getRelative(BlockFace.WEST).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.WEST);
				}else if(b.getTypeId() == 55){
					redstone = b.getRelative(BlockFace.WEST);
				}else{
					player.sendMessage("Could not find redstone... Power not changed");
					return;
				}
			b.setData((byte) 0xF, true);
				player.sendMessage("Power switched " + ChatColor.GREEN + "ON");
			}else{
				if(b.getRelative(BlockFace.NORTH).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.NORTH);
				}else if(b.getRelative(BlockFace.SOUTH).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.SOUTH);
				}else if(b.getRelative(BlockFace.EAST).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.EAST);
				}else if(b.getRelative(BlockFace.WEST).getTypeId() == 55){
					redstone = b.getRelative(BlockFace.WEST);
				}else if(b.getTypeId() == 55){
					redstone = b.getRelative(BlockFace.WEST);
				}else{
					player.sendMessage("Could not find redstone... Power not changed");
					return;
				}
			b.setData((byte) 0x0, true);
				player.sendMessage("Power switched " + ChatColor.RED + "OFF");
			}
			return;	
		}
		
		
			
			
				} else {
					
				}
					
			}
			}
	
	}
	
	
}
