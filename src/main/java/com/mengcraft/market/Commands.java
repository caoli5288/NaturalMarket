package com.mengcraft.market;

import java.io.IOException;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

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
					MarketManager.get().flush();
				} else if (args[0].equals("price")) {
					PriceTask.getTask().run();
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
			if (!downItem(id)) {
				sendError(sender, 1);
				System.out.println("Commands.DownItem.CanNotFind");
			} else {
				sendInfo(sender, 1);
				TableManager.getManager().saveTable("NaturalMarket");
			}
		} catch (NumberFormatException e) {
			sendError(sender, 1);
		}
	}

	private boolean downItem(int id) {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		List<MengRecord> one = table.find("id", id);
		if (one.isEmpty()) {
			return false;
		} else {
			table.delete(one);
			MarketManager.get().flush();
			return true;
		}
	}

	private void listItem(CommandSender sender, String string) {
		try {
			Double price = new Double(string);
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
				MengTable table = TableManager.getManager().getTable("NaturalMarket");
				MengRecord max = table.findOne("type", "max");
				if (max == null) {
					max = new MengBuilder().getEmptyRecord();
					max.put("type", "max");
					max.put("max", 0);
				}
				int id = max.getInteger("max");
				MengRecord record = new MengBuilder().getEmptyRecord();
				String items = StreamSerializer.getDefault().serializeItemStack(player.getItemInHand());
				record.put("price", price);
				record.put("id", id);
				record.put("items", items);
				max.put("max", id + 1);
				table.insert(record);
				table.update(max);
				TableManager.getManager().saveTable("NaturalMarket");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendInfo(CommandSender sender, int i) {
		switch (i) {
		case 0:
			sender.sendMessage(ChatColor.GOLD + "物品上架成功");
			break;
		case 1:
			sender.sendMessage(ChatColor.GOLD + "物品下架成功");
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
