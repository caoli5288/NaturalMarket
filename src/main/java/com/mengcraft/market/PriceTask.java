package com.mengcraft.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
		int count = 0;
		int sum = 0;
		for (MengRecord record : list) {
			count = count + 1;
			sum = sum + 1 + Math.abs(record.getInteger("sales"));
		}
		BigDecimal average = new BigDecimal(sum).divide(new BigDecimal(count), 2, RoundingMode.UP);
		for (MengRecord record : list) {
			BigDecimal adjust = new BigDecimal(record.getInteger("sales")).abs().divide(average, 2, RoundingMode.UP);
			if (adjust.compareTo(new BigDecimal(2)) > 0) {
				adjust = new BigDecimal(2);
			} else if (adjust.compareTo(new BigDecimal(0.5)) < 0.5) {
				adjust = new BigDecimal(0.5);
			}
			BigDecimal price = new BigDecimal(record.getString("price"));
			BigDecimal multi = new BigDecimal(new Random().nextInt(512)).divide(new BigDecimal("10240")).multiply(adjust);
			if (record.getInteger("sales") > 0) {
				price = price.add(price.multiply(multi));
			} else if (record.getInteger("sales") < 0) {
				price = price.subtract(price.multiply(multi));
			} else if (new Random().nextBoolean()) {
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
