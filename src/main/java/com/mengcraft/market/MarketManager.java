package com.mengcraft.market;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.utility.StreamSerializer;
import com.mengcraft.db.MengRecord;
import com.mengcraft.db.MengTable;
import com.mengcraft.db.TableManager;

public class MarketManager {
	private final static MarketManager MANAGER = new MarketManager();
	private final List<Inventory> inventories;

	public MarketManager() {
		this.inventories = new ArrayList<Inventory>();
	}

	public static MarketManager get() {
		return MANAGER;
	}

	public List<Inventory> getPages() {
		return inventories;
	}

	public void flush() {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		List<MengRecord> records = table.find();
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		kickViewers();
		getPages().clear();
		for (MengRecord record : records) {
			if (record.containsKey("items")) {
				ItemStack stack = genItemStack(record);
				if (stacks.size() < 40) {
					stacks.add(stack);
				} else {
					newInv(stacks);
					stacks.add(stack);
				}
			}
		}
		newInv(stacks);
	}

	private void kickViewers() {
		for (Inventory inventory : getPages()) {
			for (HumanEntity entity : inventory.getViewers()) {
				entity.closeInventory();
				sendError(entity, 0);
			}
		}
	}

	private void sendError(HumanEntity entity, int i) {
		sendError(NaturalMarket.get().getServer().getPlayerExact(entity.getName()), i);
	}

	private void sendError(Player player, int i) {
		switch (i) {
		case 0:
			player.sendMessage(ChatColor.RED + "商店正在更新信息");
			break;
		}
	}

	private void newInv(List<ItemStack> stacks) {
		Inventory inv = Bukkit.createInventory(null, 54, "NaturalMarket" + ":" + getPages().size());
		inv.setContents(stacks.toArray(new ItemStack[45]));
		inv.setItem(53, getNextPoint(true));
		inv.setItem(51, getNextPoint(false));
		getPages().add(inv);
		stacks.clear();
	}

	private ItemStack getNextPoint(boolean isNext) {
		ItemStack stack = new ItemStack(Material.PAPER);
		ItemMeta itemMeta = stack.getItemMeta();
		List<String> lore = new ArrayList<String>();
		if (isNext) {
			lore.add("下一页");
		} else {
			lore.add("上一页");
		}
		itemMeta.setLore(lore);
		stack.setItemMeta(itemMeta);
		return stack;
	}

	private ItemStack genItemStack(MengRecord record) {
		try {
			ItemStack stack = StreamSerializer.getDefault().deserializeItemStack(record.getString("items"));
			ItemMeta meta = stack.getItemMeta();
			List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<String>();
			lore.add(0, "卖出价格: " + record.getDouble("price") * 0.8); // 2
			lore.add(0, "买入价格: " + record.getDouble("price"));  // 1
			lore.add(0, "商品编号: " + record.getInteger("id"));  // 0
			meta.setLore(lore);
			stack.setItemMeta(meta);
			return stack;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
