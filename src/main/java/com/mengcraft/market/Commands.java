package com.mengcraft.market;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.utility.StreamSerializer;
import com.mengcraft.db.MengBuilder;
import com.mengcraft.db.MengRecord;
import com.mengcraft.db.MengTable;
import com.mengcraft.db.TableManager;

public class Commands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length < 1) {
			if (sender instanceof Player) {
				showMarket(sender);
			}
		} else if (sender.hasPermission("market.admin")) {
			if (args.length < 2) {
				if (args[0].equals("flush")) {
					if (sender.hasPermission("market.admin")) {
						MarketManager.get().flush();
					}
				}
			} else if (args.length < 3) {
				if (args[0].equals("list")) {
					listItem(sender, args[1]);
				} else if (args[0].equals("down")) {
					downItem(sender, args[1]);
				}
			}
		}
		return true;
	}

	private void downItem(CommandSender sender, String string) {
		try {
			int id = Integer.parseInt(string);
			downItem(id);
		} catch (NumberFormatException e) {
			sendError(sender, 1);
		}
	}

	private boolean downItem(int id) {
		// TODO 写好下架, 下架完开个线程重排序号, 排完刷新
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
//		MengRecord record = table.find(key, value);
		return false;
	}

	private void listItem(CommandSender sender, String string) {
		try {
			Double price = Double.parseDouble(string);
			if (price <= 0) {
				sendError(sender, 0);
			} else {
				sendInfo(sender, 0);
				newItem(sender.getName(), price);
				MarketManager.get().flush();
			}
		} catch (NumberFormatException e) {
			sendError(sender, 0);
		}
	}

	private void newItem(String name, double price) {
		Player player = NaturalMarket.get().getServer().getPlayerExact(name);
		if (player != null) {
			try {
				newItem(player.getItemInHand(), price);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void newItem(ItemStack stack, double price) throws IOException {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		MengRecord record = new MengBuilder().getEmptyRecord();
		String items = StreamSerializer.getDefault().serializeItemStack(stack);
		record.put("price", price);
		record.put("id", table.find().size());
		record.put("items", items);
		table.insert(record);
		TableManager.getManager().saveTable("NaturalMarket");
	}

	private void sendInfo(CommandSender sender, int i) {
		switch (i) {
		case 0:
			sender.sendMessage(ChatColor.GOLD + "上架成功");
			break;
		}
	}

	private void sendError(CommandSender sender, int i) {
		switch (i) {
		case 0:
			sender.sendMessage(ChatColor.RED + "价格设置错误");
			break;
		case 1:
			sender.sendMessage(ChatColor.RED + "物品选取错误");
			break;
		}
	}

	private void showMarket(CommandSender sender) {
		Player player = NaturalMarket.get().getServer().getPlayerExact(sender.getName());
		showMarket(player);
	}

	private void showMarket(Player player) {
		Inventory inv = MarketManager.get().getPages().get(0);
		player.openInventory(inv);
	}
}
