package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.List;

// When the server starts, a Banker price list from an older version of the mod is brought up to date once
// (CapitalismConfig.upgradeBankPrices): lines still at the old default take the new price, new items are added, and any
// price the server's admins changed (or an item they took off) stays as it is. Nothing is reset. Then bankPricesVersion
// says it's done, so it never runs again for that version.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class BankPriceUpgrade {

    private BankPriceUpgrade() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (CapitalismConfig.BANK_PRICES_VERSION.get() >= CapitalismConfig.BANK_PRICES_LATEST) return;
        List<? extends String> old = CapitalismConfig.BANK_PRICES.get();
        List<String> now = CapitalismConfig.upgradeBankPrices(old);
        int added = now.size() - old.size();
        int changed = 0;
        for (int i = 0; i < old.size(); i++) if (!old.get(i).equals(now.get(i))) changed++;
        CapitalismConfig.BANK_PRICES.set(now);
        CapitalismConfig.BANK_PRICES_VERSION.set(CapitalismConfig.BANK_PRICES_LATEST);
        CapitalismConfig.spec().save();
        Capitalism.LOGGER.info("Banker prices brought up to version {}: {} new items, {} prices changed (your own prices were kept)",
            CapitalismConfig.BANK_PRICES_LATEST, added, changed);
    }
}
