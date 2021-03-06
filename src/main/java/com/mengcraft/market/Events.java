package com.mengcraft.market;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class Events implements Listener {

	private final Server server;
	private final Plugin plugin;
	
	public Events(Plugin plugin) {
		this.plugin = plugin;
		this.server = plugin.getServer();
	}

	@EventHandler
	public void onMarket(InventoryClickEvent event) {
		if (event.getView().getTitle().startsWith("NaturalMarket") && event.getCurrentItem() != null) {
			if (event.getCurrentItem().getType() != Material.AIR) {
				event.setCancelled(true);
				this.server.getScheduler().runTaskLater(this.plugin, new FlushInventory(event.getWhoClicked().getName()), 1);
				if (event.getRawSlot() < 54) {
					clickAct(event);
				}
			}
		}
	}

	private void clickAct(InventoryClickEvent event) {
		if (event.getSlot() < 40) {
			if (event.getClick().equals(ClickType.LEFT)) {
				buy(event.getWhoClicked().getName(), event.getCurrentItem());
			} else if (event.getClick().equals(ClickType.RIGHT)) {
				sell(event.getWhoClicked().getName(), event.getCurrentItem());
			} else if (event.getClick().equals(ClickType.SHIFT_LEFT)) {
				down(event.getWhoClicked().getName(), event.getCurrentItem());
			}
		} else if (event.getRawSlot() == 51) {
			showNextPage(event.getWhoClicked(), event.getInventory(), false);
		} else if (event.getRawSlot() == 53) {
			showNextPage(event.getWhoClicked(), event.getInventory(), true);
		}
	}

	private void down(String name, ItemStack stack) {
		if (Bukkit.getPlayerExact(name).hasPermission("market.admin")) {
			int id = new Integer(stack.getItemMeta().getLore().get(0).split(" ")[1]);
			Bukkit.getPlayerExact(name).getInventory().addItem(NaturalMarket.MARKET_MANAGER.getStack(id));
			NaturalMarket.MARKET_MANAGER.downStack(id);
		}
	}

	private void sell(String name, ItemStack stack) {
		int id = new Integer(stack.getItemMeta().getLore().get(0).split(" ")[1]);
		// This object "item" is prototype item stack
		ItemStack item = NaturalMarket.MARKET_MANAGER.getStack(id);
		Inventory inventory = Bukkit.getPlayerExact(name).getInventory();
		Map<Integer, ItemStack> map = new HashMap<Integer, ItemStack>();
		int amount = 0;
		ItemStack[] stacks = inventory.getContents();
		for (int count = 0; count < stacks.length; count = count + 1) {
			if (compareStack(item, stacks[count])) {
				map.put(count, stacks[count]);
				amount = amount + stacks[count].getAmount();
			}
		}
		if (amount < item.getAmount()) {
			sendInfo(name, 0);
		} else {
			sendInfo(name, 1);
			pickStack(inventory, map, item.getAmount());
			double price = new Double(stack.getItemMeta().getLore().get(2).split(" ")[1]);
			NaturalMarket.getEconomy().depositPlayer(name, price);
			NaturalMarket.MARKET_MANAGER.setStackLog(id, false);
		}
	}

	private void buy(String name, ItemStack stack) {
		double price = new Double(stack.getItemMeta().getLore().get(1).split(" ")[1]);
		int id = new Integer(stack.getItemMeta().getLore().get(0).split(" ")[1]);
		// System.out.println("Events.Buy.Buyer." + name);
		if (NaturalMarket.getEconomy().has(name, price)) {
			NaturalMarket.getEconomy().withdrawPlayer(name, price);
			Player player = Bukkit.getPlayerExact(name);
			ItemStack item = NaturalMarket.MARKET_MANAGER.getStack(id);
			if (player.getInventory().addItem(item).size() > 0) {
				player.getWorld().dropItem(player.getLocation(), item);
			}
			NaturalMarket.MARKET_MANAGER.setStackLog(id, true);
		} else {
			this.server.getPlayerExact(name).sendMessage(ChatColor.RED + "账户余额不足");
		}
	}

	private void pickStack(Inventory inventory, Map<Integer, ItemStack> map, int amount) {
		int i = 0;
		for (Entry<Integer, ItemStack> entry : map.entrySet()) {
			if (entry.getValue().getAmount() < amount - i) {
				i = i + entry.getValue().getAmount();
				inventory.setItem(entry.getKey(), new ItemStack(Material.AIR));
			} else {
				if (entry.getValue().getAmount() > amount - i) {
					entry.getValue().setAmount(entry.getValue().getAmount() - amount + i);
					return;
				} else {
					inventory.setItem(entry.getKey(), new ItemStack(Material.AIR));
					return;
				}
			}
		}
	}

	private boolean compareStack(ItemStack one, ItemStack oth) {
		if (oth != null && one.getType() == oth.getType()) {
			ItemMeta oneMeta = one.getItemMeta();
			ItemMeta othMeta = oth.getItemMeta();
			if (oneMeta.toString().equals(othMeta.toString())) {
				return one.getDurability() == oth.getDurability();
			}
		}
		return false;
	}

	private void showNextPage(HumanEntity who, Inventory inventory, boolean isNext) {
		int curPage = new Integer(inventory.getTitle().split(":")[1]);
		if (isNext) {
			curPage = curPage + 1;
		} else {
			curPage = curPage - 1;
		}
		this.server.getScheduler().runTaskLater(this.plugin, new ShowMarketPage(who, curPage), 1);
	}

	private void sendInfo(String name, int i) {
		Player player = Bukkit.getPlayerExact(name);
		switch (i) {
		case 0:
			player.sendMessage(ChatColor.RED + "你没有此物品无法出售");
			break;
		case 1:
			player.sendMessage(ChatColor.GOLD + "物品出售成功");
			break;
		}
	}

	private class FlushInventory implements Runnable {
		private final String name;

		public FlushInventory(String name) {
			this.name = name;
		}

		@Override
		public void run() {
			Player player = Bukkit.getPlayerExact(getName());
			player.openInventory(player.getOpenInventory().getTopInventory());
		}

		public String getName() {
			return name;
		}
	}

	private class ShowMarketPage implements Runnable {

		private final HumanEntity name;
		private final int page;

		public ShowMarketPage(HumanEntity name, int page) {
			this.name = name;
			this.page = page > -1 ? page < NaturalMarket.MARKET_MANAGER.getPages().size() ? page : 0 : NaturalMarket.MARKET_MANAGER.getPages().size() - 1;
		}

		@Override
		public void run() {
			this.name.openInventory(NaturalMarket.MARKET_MANAGER.getPages().get(this.page));
		}

	}
}
