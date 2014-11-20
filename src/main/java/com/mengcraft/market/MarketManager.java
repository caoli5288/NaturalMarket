package com.mengcraft.market;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.utility.StreamSerializer;
import com.mengcraft.db.MengBuilder;
import com.mengcraft.db.MengRecord;
import com.mengcraft.db.MengTable;
import com.mengcraft.db.TableManager;

public class MarketManager {
	private final static MarketManager MANAGER = new MarketManager();
	private final List<Inventory> pages;

	private MarketManager() {
		this.pages = new ArrayList<Inventory>();
	}

	public static MarketManager getManager() {
		return MANAGER;
	}

	public List<Inventory> getPages() {
		return pages;
	}

	public void listStack(ItemStack stack, double price) {
		try {
			MengTable table = TableManager.getManager().getTable("NaturalMarket");
			MengRecord max = table.findOne("type", "max");
			if (max == null) {
				max = new MengBuilder().getEmptyRecord();
				max.put("type", "max");
				max.put("max", 0);
			}
			int id = max.getInteger("max");
			MengRecord record = new MengBuilder().getEmptyRecord();
			String items = StreamSerializer.getDefault().serializeItemStack(stack);
			record.put("price", price);
			record.put("id", id);
			record.put("items", items);
			max.put("max", id + 1);
			table.insert(record);
			table.update(max);
			TableManager.getManager().saveTable("NaturalMarket");
			MarketManager.getManager().flushPage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean downStack(int id) {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		List<MengRecord> one = table.find("id", id);
		if (one.isEmpty()) {
			return false;
		} else {
			table.delete(one);
			MarketManager.getManager().flushPage();
			return true;
		}
	}

	public void flushPage() {
		TableManager manager = TableManager.getManager();
		MengTable table = manager.getTable("NaturalMarket");
		List<MengRecord> records = table.find("items");
		List<ItemStack> list = new ArrayList<ItemStack>();
		List<ItemStack[]> stacks = new ArrayList<ItemStack[]>();
		for (MengRecord record : records) {
			ItemStack stack = genListedStack(record);
			if (list.size() < 40) {
				list.add(stack);
			} else {
				stacks.add(list.toArray(new ItemStack[40]));
				list.clear();
				list.add(stack);
			}
		}
		stacks.add(list.toArray(new ItemStack[40]));
		cutPage(stacks);
		// copy items from list to page
		for (int i = 0; i < stacks.size(); i = i + 1) {
			getPages().get(i).setContents(stacks.get(i));
			setPagePoint(getPages().get(i));
		}
	}

	private void cutPage(List<ItemStack[]> list) {
		if (getPages().size() > list.size()) {
			int i = getPages().size() - 1;
			for (HumanEntity entity : new ArrayList<HumanEntity>(getPages().get(i).getViewers())) {
				entity.openInventory(getPages().get(0));
			}
			getPages().get(i).clear();
			getPages().remove(i);
			cutPage(list);
		} else if (getPages().size() < list.size()) {
			getPages().add(Bukkit.createInventory(null, 54, "NaturalMarket:" + getPages().size()));
			cutPage(list);
		}
	}

	private void setPagePoint(Inventory inventory) {
		ItemStack prev = new ItemStack(Material.MELON);
		ItemStack next = new ItemStack(Material.SPECKLED_MELON);
		ItemMeta prevMeta = prev.getItemMeta();
		prevMeta.setDisplayName(ChatColor.GOLD + "上一页");
		ItemMeta nextMeta = next.getItemMeta();
		nextMeta.setDisplayName(ChatColor.GOLD + "下一页");
		prev.setItemMeta(prevMeta);
		next.setItemMeta(nextMeta);
		inventory.setItem(53, next);
		inventory.setItem(51, prev);
	}

	private ItemStack genListedStack(MengRecord record) {
		try {
			ItemStack stack = StreamSerializer.getDefault().deserializeItemStack(record.getString("items"));
			ItemMeta meta = stack.getItemMeta();
			List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<String>();
			String price = record.getString("price");
			lore.add(0, "卖出价格: " + new BigDecimal(price).multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.UP)); // 2
			lore.add(0, "买入价格: " + new BigDecimal(price).setScale(2, RoundingMode.UP)); // 1
			lore.add(0, "商品编号: " + record.getInteger("id")); // 0
			meta.setLore(lore);
			stack.setItemMeta(meta);
			return stack;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
