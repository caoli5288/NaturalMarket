package com.mengcraft.market;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Events implements Listener {
	@EventHandler
	public void onMarket(InventoryClickEvent event) {
		if (event.getClickedInventory() != null && event.getView().getTitle().startsWith("NaturalMarket") && event.getCurrentItem().getType() != Material.AIR) {
			event.setCancelled(true);
			Bukkit.getScheduler().runTaskLater(NaturalMarket.get(), new FlushInventory(event.getWhoClicked().getName()), 1);
			if (event.getClickedInventory().getTitle().startsWith("NaturalMarket")) {
				clickAct(event);
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
		} else if (event.getSlot() == 51) {
			showNextPage(event.getWhoClicked(), event.getClickedInventory(), false);
		} else if (event.getSlot() == 53) {
			showNextPage(event.getWhoClicked(), event.getClickedInventory(), true);
		}
	}

	private void down(String name, ItemStack stack) {
		if (Bukkit.getPlayerExact(name).hasPermission("market.admin")) {
			int id = new Integer(stack.getItemMeta().getLore().get(0).split(" ")[1]);
			Bukkit.getPlayerExact(name).getInventory().addItem(MarketManager.getManager().getStack(id));
			MarketManager.getManager().downStack(id);
		}
	}

	private void sell(String name, ItemStack stack) {
		int id = new Integer(stack.getItemMeta().getLore().get(0).split(" ")[1]);
		// This object "item" is prototype item stack
		ItemStack item = MarketManager.getManager().getStack(id);
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
			double price = new Double(stack.getItemMeta().getLore().get(1).split(" ")[1]);
			NaturalMarket.getEconomy().depositPlayer(name, price);
			MarketManager.getManager().setStackLog(id, false);
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

	private boolean compareStack(ItemStack item, ItemStack stack) {
		if (stack != null && item.getType().equals(stack.getType())) {
			ItemMeta itemMeta = item.getItemMeta();
			ItemMeta stackMeta = stack.getItemMeta();
			if (itemMeta.toString().equals(stackMeta.toString())) {
				return true;
			}
		}
		return false;
	}

	private void buy(String name, ItemStack stack) {
		double price = new Double(stack.getItemMeta().getLore().get(1).split(" ")[1]);
		int id = new Integer(stack.getItemMeta().getLore().get(0).split(" ")[1]);
		// System.out.println("Events.Buy.Buyer." + name);
		if (NaturalMarket.getEconomy().has(name, price)) {
			NaturalMarket.getEconomy().withdrawPlayer(name, price);
			Player player = Bukkit.getPlayerExact(name);
			ItemStack item = MarketManager.getManager().getStack(id);
			if (player.getInventory().addItem(item).size() > 0) {
				player.getWorld().dropItem(player.getLocation(), item);
			}
			MarketManager.getManager().setStackLog(id, true);
		} else {
			NaturalMarket.get().getServer().getPlayerExact(name).sendMessage(ChatColor.RED + "账户余额不足");
		}
	}

	private void showNextPage(HumanEntity who, Inventory inventory, boolean isNext) {
		int curPage = new Integer(inventory.getTitle().split(":")[1]);
		if (isNext) {
			curPage = curPage + 1;
		} else {
			curPage = curPage - 1;
		}
		NaturalMarket.get().getServer().getScheduler().runTaskLater(NaturalMarket.get(), new ShowMarketPage(who.getName(), curPage), 1);
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
		private final String name;
		private final int page;

		public ShowMarketPage(String name, int page) {
			this.name = name;
			this.page = page > -1 ? page < MarketManager.getManager().getPages().size() ? page : 0 : MarketManager.getManager().getPages().size() - 1;
		}

		@Override
		public void run() {
			NaturalMarket.get().getServer().getPlayerExact(getName()).openInventory(MarketManager.getManager().getPages().get(getPage()));
		}

		private int getPage() {
			return page;
		}

		private String getName() {
			return name;
		}
	}
}
