package me.ZephireNZ.NoirlandAutoPromote;

import me.ZephireNZ.NoirlandAutoPromote.commands.Command;
import me.ZephireNZ.NoirlandAutoPromote.commands.CommandAgree;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NoirlandAutoPromote extends JavaPlugin {
	
	public final ArrayList<PlayerTimeObject> playerTimeArray = new ArrayList<PlayerTimeObject>();
	public DatabaseHandler dbHandler;
    public GMHandler gmHandler;
    public ConfigHandler confHandler;
    public PromotionHandler pmHandler;
    private BukkitTask saveTimesTask;
	
	@Override
	public void onEnable(){
		confHandler = new ConfigHandler(this);
		dbHandler = new DatabaseHandler(this);
		gmHandler = new GMHandler(this);
		pmHandler = new PromotionHandler(this);
		
		for(Player player : getServer().getOnlinePlayers()) {
			PlayerTimeObject pto = new PlayerTimeObject(player); // Add online players to pto object after reload
			pto.setJoinTime();
			playerTimeArray.add(pto);
			
			pmHandler.checkForPromotion(player);
		}
		
		startSaveTimes();
        dbHandler.refreshCachedRanks();
		
		this.getCommand("autopromote").setExecutor(new Command(this));
        this.getCommand("agree").setExecutor((new CommandAgree(this)));
		
		getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
		getServer().getPluginManager().registerEvents(gmHandler, this);
		
		PluginDescriptionFile pdfFile = this.getDescription();
		getLogger().info(pdfFile.getName() + " version " + pdfFile.getVersion() + " started.");
	}
	
	@Override
	public void onDisable(){
		saveToDB(false); // Save temporary playTimes to Database
		dbHandler.closeConnection();
		
		PluginDescriptionFile pdfFile = this.getDescription();
		getLogger().info(pdfFile.getName() + " version " + pdfFile.getVersion() + " stopped.");
	}
	
	public void saveToDB(boolean reload) {
		for(PlayerTimeObject pto : playerTimeArray) {
			String player = pto.getPlayer().getName();
			pto.setQuitTime();
			dbHandler.setPlayTime(player, pto.getPlayTime() + dbHandler.getPlayTime(player));
			dbHandler.setTotalPlayTime(player, pto.getPlayTime() + dbHandler.getTotalPlayTime(player));
			pto.setJoinTime();
			pto.resetQuitTime();
		}
        if(reload) {
            dbHandler.refreshCachedRanks();
        }
	}
	
	public void reload() {
		saveToDB(true);
		confHandler.loadConfig();
		confHandler.config = this.getConfig();
		for(Player player : getServer().getOnlinePlayers()) {
			pmHandler.checkForPromotion(player);
		}
        startSaveTimes();
		getLogger().info("Plugin reloaded successfully.");
	}
	
	public void debug(String message) {
		if(confHandler.getDebug()) {
			getLogger().info("[DEBUG] " + message);
		}
	}
	
	public String formatTime(long millis) {
		long hour = TimeUnit.HOURS.toMillis(1);
		long day = hour * 24;
		
		if(millis < hour) {
			long mins = TimeUnit.MILLISECONDS.toMinutes(millis);
			return (mins + " Minutes");
		}
		else if(millis < day) {
			long hours = TimeUnit.MILLISECONDS.toHours(millis);
			return (hours + " Hours");
		}
		else{
			long days = TimeUnit.MILLISECONDS.toDays(millis);
			long subtrDays = TimeUnit.DAYS.toMillis(days);
			long hours = TimeUnit.MILLISECONDS.toHours(millis - subtrDays);
			return (days + " Days, " + hours + " Hours");
		}
	}
	
	public void sendMessage(CommandSender sender, String msg, Boolean prefix) {
        if(prefix) {
            msg = ChatColor.RED + "[NoirPromote] " + ChatColor.RESET + msg;
        }
		if(sender instanceof Player) {
			Player pSender = (Player) sender;
			pSender.sendMessage(msg);
		}else{
			getLogger().info(ChatColor.stripColor(msg));
		}
	}

    void startSaveTimes() {
        if(saveTimesTask != null) {
            saveTimesTask.cancel();
        }
        saveTimesTask = new SaveTimesTask(this).runTaskTimer(this, confHandler.getSaveTimeSeconds() * 20L, confHandler.getSaveTimeSeconds() * 20L);
    }
}

class SaveTimesTask extends BukkitRunnable {
	
	private final NoirlandAutoPromote plugin;
	
	public SaveTimesTask(NoirlandAutoPromote plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run() {
		for(Player player : plugin.getServer().getOnlinePlayers()) { // Check for promoteable player
			plugin.pmHandler.checkForPromotion(player);
		}
        plugin.saveToDB(true); // Save all players to DB on event
	}
}
