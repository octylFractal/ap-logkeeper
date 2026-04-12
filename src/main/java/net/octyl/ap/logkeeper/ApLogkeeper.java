/*
 * SPDX-FileCopyrightText: Octavia Togami <octy@octyl.net>
 * SPDX-License-Identifier: MPL-2.0
 */

package net.octyl.ap.logkeeper;

import io.github.archipelagomw.Client;
import io.github.archipelagomw.flags.ItemsHandling;
import org.tinylog.Logger;
import org.tinylog.jul.JulTinylogBridge;
import picocli.CommandLine;

import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

class ApLogkeeper {

    static void main(String... args) {
        JulTinylogBridge.activate();
        System.exit(
            new CommandLine(
                CommandLine.getCommandMethods(ApLogkeeper.class, "execute").getFirst()
            )
                .registerConverter(ApServer.class, new ApServer.ApServerConverter())
                .execute(args)
        );
    }

    @CommandLine.Command(
        name = "ap-logkeeper",
        description =
            """
            A minimal tool for tracking specific logs from an Archipelago multiworld.
            """,
        mixinStandardHelpOptions = true
    )
    public int execute(
        @CommandLine.Parameters(description = "The server address (host:port) to connect to.")
        ApServer server,
        @CommandLine.Parameters(arity = "1..*", description = "The usernames whose sent items should be tracked.")
        List<String> usernames,
        @CommandLine.Option(
            names = {"-e", "--exclude"},
            paramLabel = "<excludePattern>",
            description =
                """
                Exclude the specified items from being output.
                Interpreted as a regular expression.
                """,
            split = ","
        )
        List<Pattern> excludedItems
    ) throws URISyntaxException {
        if (excludedItems == null) {
            excludedItems = List.of();
        }
        Logger.info("Tracking the following usernames: " + usernames);
        Logger.info("Excluding items matching the following patterns: " + excludedItems.stream().map(Pattern::pattern).toList());
        Client client = new EnhancedClient();
        client.getEventManager().registerListener(
            new ItemPrinter(client, new LinkedHashSet<>(usernames), excludedItems)
        );
        client.setGame("");
        client.setName(usernames.getFirst());
        client.setItemsHandlingFlags(
            ItemsHandling.SEND_ITEMS | ItemsHandling.SEND_OWN_ITEMS | ItemsHandling.SEND_STARTING_INVENTORY
        );
        client.setTags(Set.of("TextOnly"));
        client.connect(server.hostname() + ":" + server.port());
        // Die forever.
        while (true) {
            try {
                //noinspection BusyWait
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                return 0;
            }
        }
    }

    private static final class EnhancedClient extends Client {
        private final Timer reconnectTimer = new Timer("Reconnect Timer", true);
        private final AtomicReference<TimerTask> reconnectTask = new AtomicReference<>();

        @Override
        public void onError(Exception ex) {
            // It's already logged.
        }

        @Override
        public void onClose(String reason, int attemptingReconnect) {
            String reconnectMsg = attemptingReconnect == 0
                ? "" : "(attempting reconnect in " + attemptingReconnect + "s)";
            Logger.warn("Connection closed: " + reason + reconnectMsg);
            if (attemptingReconnect == 0) {
                Logger.info("Attempting manual reconnect in 10s...");
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        Logger.info("Attempting manual reconnect...");
                        reconnectTask.set(null);
                        try {
                            reconnect();
                        } catch (Exception e) {
                            Logger.error("Manual reconnect failed: " + e.getMessage());
                        }
                    }
                };
                if (reconnectTask.compareAndSet(null, task)) {
                    reconnectTimer.schedule(task, 10_000);
                } else {
                    Logger.warn("Reconnect already scheduled, skipping.");
                }
            } else {
                Logger.info("Waiting for automatic reconnect...");
                TimerTask task = reconnectTask.get();
                if (task != null) {
                    task.cancel();
                }
            }
        }
    }
}
