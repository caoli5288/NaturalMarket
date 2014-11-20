package com.mengcraft.market;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Commands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length < 1) {
			showMarket(sender);
		} else if (sender.hasPermission("market.admin")) {
			adminCommand(sender, args);
		}
		return true;
	}

	private void adminCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			if (args[0].equals("flush")) {
				MarketManager.getManager().flushPage();
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

	private void downItem(CommandSender sender, String string) {
		try {
			int id = Integer.parseInt(string);
			if (MarketManager.getManager().downStack(id)) {
				sendInfo(sender, 1);
			} else {
				sendInfo(sender, 3);
				// System.out.println("Commands.DownItem.CanNotFind");
			}
		} catch (NumberFormatException e) {
			sendInfo(sender, 3);
		}
	}

	private void listItem(CommandSender sender, String string) {
		try {
			Double price = new Double(string);
			if (price <= 0) {
				sendInfo(sender, 2);
			} else {
				sendInfo(sender, 0);
				ItemStack stack = Bukkit.getPlayerExact(sender.getName()).getItemInHand();
				MarketManager.getManager().listStack(stack, price);
			}
		} catch (NumberFormatException e) {
			sendInfo(sender, 2);
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
		case 2:
			sender.sendMessage(ChatColor.RED + "价格设置错误");
			break;
		case 3:
			sender.sendMessage(ChatColor.RED + "物品选取错误");
			break;
		}
	}

	private void showMarket(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = NaturalMarket.get().getServer().getPlayerExact(sender.getName());
			showMarket(player);
		}
	}

	private void showMarket(Player player) {
		player.openInventory(MarketManager.getManager().getPages().get(0));
	}
}
