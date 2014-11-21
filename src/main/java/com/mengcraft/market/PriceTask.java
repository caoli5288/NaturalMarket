package com.mengcraft.market;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;

import com.mengcraft.db.MengRecord;
import com.mengcraft.db.MengTable;
import com.mengcraft.db.TableManager;

public class PriceTask implements Runnable {

	private final static PriceTask TASK = new PriceTask();

	private PriceTask() {
	}

	@Override
	public void run() {
		if (Bukkit.getOnlinePlayers().length < 1) {
			return;
		}
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		List<MengRecord> list = table.find("price");
		for (MengRecord record : list) {
			BigDecimal price = new BigDecimal(record.getString("price"));
			BigDecimal multi = new BigDecimal(new Random().nextInt(1024)).divide(new BigDecimal("10240"));
			if (record.getInteger("sales") > 0) {
				price = price.add(price.multiply(multi));
			} else if (record.getInteger("sales") < 0){
				price = price.subtract(price.multiply(multi));
			} else if (new Random().nextBoolean()){
				price = price.add(price.multiply(multi));
			} else {
				price = price.subtract(price.multiply(multi));
			}
			record.put("sales", 0);
			record.put("price", price.doubleValue());
			table.update(record);
		}
		TableManager.getManager().saveTable("NaturalMarket");
		MarketManager.getManager().flushPage();
	}

	public static PriceTask getTask() {
		return TASK;
	}

}
