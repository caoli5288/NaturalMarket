package com.mengcraft.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

import com.mengcraft.db.MengRecord;
import com.mengcraft.db.MengTable;
import com.mengcraft.db.TableManager;

public class PriceTask implements Runnable {

	private final static PriceTask TASK = new PriceTask();

	private PriceTask() {
	}

	@Override
	public void run() {
		MengTable table = TableManager.getManager().getTable("NaturalMarket");
		List<MengRecord> list = table.find("price");
		for (MengRecord record : list) {
			double price = record.getDouble("price");
			double multi = new Random().nextInt(1024) / 10240d;
			if (record.getInteger("sales") > 0) {
				price = price + price * multi;
			} else {
				price = price - price * multi;
			}
			record.put("sales", 0);
			record.put("price", new BigDecimal(price).setScale(2, RoundingMode.HALF_UP).doubleValue());
			table.update(record);
		}
		TableManager.getManager().saveTable("NaturalMarket");
		MarketManager.getManager().flush();
	}

	public static PriceTask getTask() {
		return TASK;
	}

}
