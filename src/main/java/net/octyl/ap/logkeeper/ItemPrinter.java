/*
 * SPDX-FileCopyrightText: Octavia Togami <octy@octyl.net>
 * SPDX-License-Identifier: MPL-2.0
 */

package net.octyl.ap.logkeeper;

import io.github.archipelagomw.Client;
import io.github.archipelagomw.Print.APPrintJsonType;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.PrintJSONEvent;
import io.github.archipelagomw.flags.NetworkItem;
import io.github.archipelagomw.parts.DataPackage;
import io.github.archipelagomw.parts.NetworkSlot;
import org.tinylog.Logger;

import java.util.List;
import java.util.SequencedSet;
import java.util.regex.Pattern;

public final class ItemPrinter {
    private final Client client;
    private final SequencedSet<String> usernames;
    private final List<Pattern> excludedItems;

    public ItemPrinter(
        Client client,
        SequencedSet<String> usernames,
        List<Pattern> excludedItems
    ) {
        this.client = client;
        this.usernames = usernames;
        this.excludedItems = excludedItems;
    }

    @ArchipelagoEventListener
    public void onPrintJson(PrintJSONEvent event) {
        if (event.type != APPrintJsonType.ItemSend) {
            return;
        }
        if ((event.item.flags & NetworkItem.ADVANCEMENT) == 0) {
            Logger.debug(
                "Excluded item send {} because it is not an advancement",
                event.apPrint.getPlainText()
            );
            return;
        }
        String playerName = event.item.playerName;
        if (!usernames.contains(playerName)) {
            Logger.debug(
                "Excluded item send {} because player {} is not in usernames",
                event.apPrint.getPlainText(),
                playerName
            );
            return;
        }
        NetworkSlot targetSlot = client.getSlotInfo().get(event.player);
        if (usernames.contains(targetSlot.name)) {
            Logger.debug(
                "Excluded item send {} because target slot {} is in usernames",
                event.apPrint.getPlainText(),
                targetSlot.name
            );
            return;
        }
        String itemName = resolveItemName(targetSlot, event.item);
        if (excludedItems.stream().anyMatch(pattern -> pattern.matcher(itemName).matches())) {
            Logger.debug(
                "Excluded item send {} because item {} matches an exclude pattern",
                event.apPrint.getPlainText(),
                itemName
            );
            return;
        }
        System.out.println(event.apPrint.getPlainText());
    }

    private String resolveItemName(NetworkSlot targetSlot, io.github.archipelagomw.parts.NetworkItem item) {
        DataPackage dp = client.getDataPackage();
        return dp.getItem(item.itemID, targetSlot.game);
    }

    @Override
    public String toString() {
        return "ItemPrinter{" +
            "usernames=" + usernames +
            ",excludedItems=" + excludedItems.stream().map(Pattern::pattern).toList() +
            '}';
    }
}
