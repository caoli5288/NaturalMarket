package com.mengcraft.market;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class Events implements Listener {

	@EventHandler
	public void onMarket(InventoryClickEvent event) {
		if (event.getClickedInventory() != null && event.getClickedInventory().getTitle().startsWith("NaturalMarket")) {
			System.out.println("Events.OnMarket.Hitting");
			System.out.println("Events.OnMarket.Slot." + event.getSlot());
			event.setCancelled(true);
			if (event.getSlot() > -1) {
				if (event.getSlot() < 40) {

				} else {
					switch (event.getSlot()) {
					// 上一页
					case 51:
						showNextPage(event.getWhoClicked(), event.getClickedInventory(), false);
						break;
					// 下一页
					case 53:
						showNextPage(event.getWhoClicked(), event.getClickedInventory(), true);
						break;
					}
				}
			}
		}
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
