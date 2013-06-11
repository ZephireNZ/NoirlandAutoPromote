package me.ZephireNZ.NoirlandAutoPromote;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Iterator;

class PlayerJoinQuitListener implements Listener {
	
	private final NoirlandAutoPromote plugin;
	private final DatabaseHandler dbHandler;
	private final PromotionHandler pmHandler;
	
	public PlayerJoinQuitListener(NoirlandAutoPromote plugin) {
		this.plugin = plugin;
		this.dbHandler = plugin.dbHandler;
		this.pmHandler = plugin.pmHandler;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		plugin.debug("Player " + player.getName() + " joined.");
		dbHandler.checkForPlayer(player.getName());
		PlayerTimeObject pto = new PlayerTimeObject(player);
		pto.setJoinTime();
		plugin.debug(player.getName()+ " join: " + pto.getJoinTime());
		plugin.playerTimeArray.add(pto);
		pmHandler.checkForPromotion(player);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		plugin.debug("Player " + player.getName() + " left.");
		for(Iterator<PlayerTimeObject> it = plugin.playerTimeArray.iterator(); it.hasNext(); ) {
			PlayerTimeObject pto = it.next();
			if(pto.getPlayer() == player ) {
				pto.setQuitTime();
				plugin.debug(player.getName()+ " quit: " + pto.getQuitTime());
				dbHandler.setPlayTime(player.getName(), dbHandler.getPlayTime(player.getName()) + pto.getPlayTime());
				plugin.debug(player.getName()+ " playtime: " + dbHandler.getPlayTime(player.getName()));
				dbHandler.setTotalPlayTime(player.getName(), dbHandler.getTotalPlayTime(player.getName()) + pto.getPlayTime());
				plugin.debug(player.getName()+ " join: " + dbHandler.getTotalPlayTime(player.getName()));
				it.remove();
			}
		}
    dbHandler.refreshCachedRanks();
	}
}
