package com.mengcraft.bukkit.market;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

	public List<Inventory> getInvs() {
		return inventories;
	}

	public void flush() {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		Iterator<MengRecord> records = table.find().iterator();
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		getInvs().clear();
		while (records.hasNext()) {
			ItemStack stack = genItemStack(records.next());
			if (stacks.size() < 40) {
				stacks.add(stack);
			} else {
				newInv(stacks);
				stacks.add(stack);
			}
		}
		newInv(stacks);
	}

	private void newInv(List<ItemStack> stacks) {
		Inventory inv = Bukkit.createInventory(null, 54, "NaturalMarket" + ":" + getInvs().size());
		inv.setContents(stacks.toArray(new ItemStack[45]));
		inv.setItem(53, getNextPoint(true));
		inv.setItem(51, getNextPoint(false));
		getInvs().add(inv);
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
			lore.add(0, "卖出价格: " + record.getDouble("price") * 0.8);
			lore.add(0, "买入价格: " + record.getDouble("price"));
			lore.add(0, "商品编号: " + record.getInteger("id"));
			meta.setLore(lore);
			stack.setItemMeta(meta);
			return stack;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
