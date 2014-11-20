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
	private boolean lock;

	private MarketManager() {
		this.inventories = new ArrayList<Inventory>();
		setLock(false);
	}

	public static MarketManager getManager() {
		return MANAGER;
	}

	public List<Inventory> getPages() {
		return inventories;
	}

	public void flush() {
		setLock(true);
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		List<MengRecord> records = table.find("items");
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		kickViewers();
		getPages().clear();
		for (MengRecord record : records) {
			ItemStack stack = genItemStack(record);
			if (stacks.size() < 40) {
				stacks.add(stack);
			} else {
				newInv(stacks);
				stacks.add(stack);
			}
		}
		newInv(stacks);
		setLock(false);
	}

	private void kickViewers() {
		for (Inventory inventory : getPages()) {
			List<HumanEntity> entities = new ArrayList<HumanEntity>(
					inventory.getViewers());
			for (HumanEntity entity : entities) {
				entity.closeInventory();
				sendError(entity, 0);
			}
		}
	}

	private void sendError(HumanEntity entity, int i) {
		sendError(
				NaturalMarket.get().getServer()
						.getPlayerExact(entity.getName()), i);
	}

	private void sendError(Player player, int i) {
		switch (i) {
		case 0:
			player.sendMessage(ChatColor.RED + "商店正在更新信息");
			break;
		}
	}

	private void newInv(List<ItemStack> stacks) {
		Inventory inv = Bukkit.createInventory(null, 54, "NaturalMarket" + ":"
				+ getPages().size());
		inv.setContents(stacks.toArray(new ItemStack[45]));
		inv.setItem(53, getNextPoint(true));
		inv.setItem(51, getNextPoint(false));
		getPages().add(inv);
		stacks.clear();
	}

	private ItemStack getNextPoint(boolean isNext) {
		ItemStack stack = new ItemStack(Material.MELON);
		ItemMeta meta = stack.getItemMeta();
		if (isNext) {
			stack.setType(Material.SPECKLED_MELON);
			meta.setDisplayName(ChatColor.GOLD + "下一页");
		} else {
			meta.setDisplayName(ChatColor.GOLD + "上一页");
		}
		stack.setItemMeta(meta);
		return stack;
	}

	private ItemStack genItemStack(MengRecord record) {
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

	public boolean isLock() {
		return lock;
	}

	public void setLock(boolean lock) {
		this.lock = lock;
	}
}
