package com.mengcraft.market;

import java.io.IOException;
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

import com.comphenix.protocol.utility.StreamSerializer;
import com.mengcraft.db.MengRecord;
import com.mengcraft.db.MengTable;
import com.mengcraft.db.TableManager;

public class Events implements Listener {

	@EventHandler
	public void onMarket(InventoryClickEvent event) {
		if (event.getClickedInventory() != null && event.getView().getTitle().startsWith("NaturalMarket")) {
			// System.out.println("Events.OnMarket.Hitting");
			// System.out.println("Events.OnMarket.Slot." + event.getSlot());
			event.setCancelled(true);
			if (event.getClickedInventory().getTitle().startsWith("NaturalMarket")) {
				marketAct(event);
			}
		}
	}

	private void marketAct(InventoryClickEvent event) {
		if (event.getCurrentItem().getType() != Material.AIR) {
			if (event.getSlot() < 40) {
				if (event.getClick().equals(ClickType.LEFT)) {
					buy(event.getWhoClicked().getName(), event.getCurrentItem());
				} else if (event.getClick().equals(ClickType.RIGHT)) {
					sell(event.getWhoClicked().getName(), event.getCurrentItem());
				}
			} else if (event.getSlot() == 51) {
				showNextPage(event.getWhoClicked(), event.getClickedInventory(), false);
			} else if (event.getSlot() == 53) {
				showNextPage(event.getWhoClicked(), event.getClickedInventory(), true);
			}
		}
	}

	private void sell(String name, ItemStack stack) {
		int id = new Integer(stack.getItemMeta().getLore().get(0).split(" ")[1]);
		// This object "item" is prototype item stack
		ItemStack item = getStack(id);
		Inventory inventory = Bukkit.getPlayerExact(name).getInventory();
		Map<Integer, ItemStack> map = new HashMap<Integer, ItemStack>();
		int amount = 0;
		ItemStack[] stacks = inventory.getContents();
		for (int count = 0; count < stacks.length; count = count + 1) {
			if (compareItemStack(item, stacks[count])) {
				map.put(count, stacks[count]);
				amount = amount + stacks[count].getAmount();
			}
		}
		if (amount < item.getAmount()) {
			sendInfo(name, 0);
		} else {
			cutItem(inventory, map, item.getAmount());
			double price = new Double(stack.getItemMeta().getLore().get(1).split(" ")[1]);
			NaturalMarket.getEconomy().depositPlayer(name, price);
			logMarket(id, false);
		}
	}

	private void cutItem(Inventory inventory, Map<Integer, ItemStack> map, int amount) {
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

	private void sendInfo(String name, int i) {
		Player player = Bukkit.getPlayerExact(name);
		switch (i) {
		case 0:
			player.sendMessage(ChatColor.RED + "你没有此物品无法出售");
			break;
		}
	}

	private boolean compareItemStack(ItemStack item, ItemStack stack) {
		if (stack != null && item.getType().equals(stack.getType())) {
			ItemMeta itemMeta = item.getItemMeta();
			ItemMeta stackMeta = stack.getItemMeta();
			if (itemMeta == null && stackMeta == null) {
				return true;
			} else if (itemMeta != null && stackMeta != null) {
				if (itemMeta.toString().equals(stackMeta.toString())) {
					return true;
				}
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
			ItemStack item = getStack(id);
			if (player.getInventory().addItem(item).size() > 0) {
				player.getWorld().dropItem(player.getLocation(), item);
			}
			logMarket(id, true);
		} else {
			NaturalMarket.get().getServer().getPlayerExact(name).sendMessage(ChatColor.RED + "账户余额不足");
		}
	}

	private ItemStack getStack(int i) {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		MengRecord record = table.find("id", i).get(0);
		try {
			return StreamSerializer.getDefault().deserializeItemStack(record.getString("items"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void logMarket(int i, boolean isBuy) {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		MengRecord record = table.find("id", i).get(0);
		if (isBuy) {
			record.put("sales", record.getInteger("sales") + 1);
		} else {
			record.put("sales", record.getInteger("sales") - 1);
		}
		table.update(record);
	}

	private void showNextPage(HumanEntity who, Inventory inventory, boolean isNext) {
		int curPage = new Integer(inventory.getTitle().split(":")[1]);
		if (isNext) {
			curPage = curPage + 1;
		} else {
			curPage = curPage - 1;
		}
		NaturalMarket.get().getServer().getScheduler().runTaskLater(NaturalMarket.get(), new ShowNewPage(who.getName(), curPage), 1);
	}

	private class ShowNewPage implements Runnable {
		private final String name;
		private final int page;

		public ShowNewPage(String name, int page) {
			this.name = name;
			this.page = page > -1 ? page < MarketManager.get().getPages().size() ? page : 0 : MarketManager.get().getPages().size() - 1;
		}

		@Override
		public void run() {
			NaturalMarket.get().getServer().getPlayerExact(getName()).openInventory(MarketManager.get().getPages().get(getPage()));
		}

		private int getPage() {
			return page;
		}

		private String getName() {
			return name;
		}
	}
}
