package com.mengcraft.bukkit;

import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import com.mengcraft.bukkit.market.Commands;
import com.mengcraft.bukkit.market.MarketManager;

public class NaturalMarket extends JavaPlugin {

	private static NaturalMarket market;

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
		MarketManager.get().flush();
	}

	public static NaturalMarket get() {
		return market;
	}

	private static void setMarket(NaturalMarket market) {
		NaturalMarket.market = market;
	}
}
