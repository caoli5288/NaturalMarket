package com.mengcraft.market;

import java.io.IOException;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

public class NaturalMarket extends JavaPlugin {

	public final static PriceTask PRICE_TASK = new PriceTask();
	public final static MarketManager MARKET_MANAGER = new MarketManager();

	private static Economy economy;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		saveConfig();
		try {
			new Metrics(this).start();
		} catch (IOException e) {
			getLogger().warning("Can not link to mcstats.org!");
		}
		getServer().getScheduler().runTaskTimer(this, PRICE_TASK, 36000, 36000);
		getCommand("market").setExecutor(new Commands());
		getServer().getPluginManager().registerEvents(new Events(this), this);
		MARKET_MANAGER.flushPage();
		setupEconomy();
		getLogger().info("梦梦家高性能服务器出租");
		getLogger().info("http://shop105595113.taobao.com");
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			setEconomy(economyProvider.getProvider());
		}
		return (getEconomy() != null);
	}

	public static Economy getEconomy() {
		return economy;
	}

	public static void setEconomy(Economy economy) {
		NaturalMarket.economy = economy;
	}
}
