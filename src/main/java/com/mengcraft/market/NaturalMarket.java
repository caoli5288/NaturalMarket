package com.mengcraft.market;

import java.io.IOException;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

public class NaturalMarket extends JavaPlugin {

	private static NaturalMarket market;
	private static Economy economy;

	@Override
	public void onLoad() {
		setMarket(this);
	}

	@Override
	public void onEnable() {
		try {
			new Metrics(this).start();
		} catch (IOException e) {
			getLogger().warning("Can not link to mcstats.org!");
		}
		getCommand("market").setExecutor(new Commands());
		getServer().getPluginManager().registerEvents(new Events(), this);
		MarketManager.get().flush();
		setupEconomy();
	}

	public static NaturalMarket get() {
		return market;
	}

	private static void setMarket(NaturalMarket market) {
		NaturalMarket.market = market;
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
