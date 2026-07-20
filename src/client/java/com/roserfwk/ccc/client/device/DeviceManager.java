package com.roserfwk.ccc.client.device;

import com.mojang.datafixers.util.Pair;
import com.roserfwk.ccc.client.CustomCoyoteControlClient;
import com.roserfwk.ccc.data.rules.Rule;
import com.roserfwk.ccc.data.waveforms.Waveform;
import com.roserfwk.ccc.network.ClientboundTriggerCoyotePacket;
import moe.prwk.btleplug4j.Adapter;
import moe.prwk.btleplug4j.BleManager;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class DeviceManager {
    public static final DeviceManager INSTANCE = new DeviceManager();

    public static final int BASE_STRENGTH = 80;
    public static final int[] INITIAL_FREQ = new int[]{10, 10, 10, 10};
    public static final int[] INITIAL_STRENGTH = new int[]{0, 0, 0, 0};

    private final BleManager bleManager;
    private final Adapter central;
    private CoyoteV3Device coyote = null;
    private boolean connected = false;
    private final ScheduledExecutorService ticker = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    public DeviceManager() {
        bleManager = new BleManager();
        Adapter adapter;
        try {
            adapter = bleManager.getAdapters().join().getFirst();
        } catch (NoSuchElementException e) {
            CustomCoyoteControlClient.LOGGER.error("No adapter found", e);
            adapter = null;
        }
        central = adapter;
    }

    public CompletableFuture<Boolean> connectAndInit() {
        if (central == null) return CompletableFuture.completedFuture(false);

        return central.startScan()
                .thenCompose(_ -> central.getPeripherals())
                .thenApply(peripherals -> {
                    var peripheral = peripherals.stream()
                            .filter(p -> p.name().equals(CoyoteV3Device.NAME))
                            .findFirst();

                    peripheral.ifPresent(value -> {
                        CustomCoyoteControlClient.LOGGER.info("Found Coyote device");
                        coyote = new CoyoteV3Device(value);
                    });

                    return peripheral.isPresent();
                }).thenCompose(result -> {
                    if (result && coyote != null) {
                        return coyote.initialize().thenApply(_ -> {
                            coyote.updateWaveformA(INITIAL_FREQ, INITIAL_STRENGTH);
                            coyote.updateWaveformB(INITIAL_FREQ, INITIAL_STRENGTH);
                            coyote.setStrengthA(BASE_STRENGTH);
                            coyote.setStrengthB(BASE_STRENGTH);
                            CustomCoyoteControlClient.LOGGER.info("Initialized Coyote device");
                            connected = true;
                            return true;
                        });
                    }

                    return CompletableFuture.completedFuture(result);
                }).exceptionally(e -> {
                    CustomCoyoteControlClient.LOGGER.error("Failed to initialize Coyote device", e);
                    return false;
                });
    }

    public void handlePayload(ClientboundTriggerCoyotePacket payload) {
        if (coyote == null) return;

        var strength = (int) (BASE_STRENGTH * payload.coefficient());

        switch (payload.channel()) {
            case A -> coyote.setStrengthA(strength);
            case B -> coyote.setStrengthB(strength);
        }

        var interval = payload.interval();
        var runIndefinitely = payload.interval() <= 0;

        var waveformTaskId = "waveform-" + payload.channel();
        schedule(waveformTaskId, new WaveformExecutor(
                waveformTaskId, interval, this, payload.channel(), payload.waveform(), runIndefinitely
        ));

        if (!runIndefinitely) {
            ticker.schedule(() -> {
                switch (payload.channel()) {
                    case A -> coyote.setStrengthA(BASE_STRENGTH);
                    case B -> coyote.setStrengthB(BASE_STRENGTH);
                }
            }, interval * 100, TimeUnit.MILLISECONDS);
        }
    }

    private void schedule(String id, Runnable task) {
        var handle = ticker.scheduleAtFixedRate(task, 0, 100, TimeUnit.MILLISECONDS);
        activeTasks.put(id, handle);
    }

    public void detachDevice() {
        if (coyote != null) {
            coyote.close();
            coyote = null;
        }

        if (!activeTasks.isEmpty()) {
            activeTasks.values().forEach(activeTask ->
                    activeTask.cancel(true));
            activeTasks.clear();
        }

        connected = false;
    }

    public void detach() {
        detachDevice();
        ticker.shutdown();

        if (central != null) {
            central.close();
        }

        if (bleManager != null) {
            bleManager.close();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private static class WaveformExecutor implements Runnable {
        private final String id;
        private final long maxRun;
        private final DeviceManager device;
        private final Rule.Channel channel;
        private final List<Pair<int[], int[]>> waveform;
        private final AtomicLong counter;
        private final boolean runIndefinitely;

        public WaveformExecutor(
                String id,
                long maxRun,
                DeviceManager device,
                Rule.Channel channel,
                Waveform waveform,
                boolean runIndefinitely
        ) {
            this.id = id;
            this.maxRun = maxRun >= 0 ? maxRun : 0;
            this.device = device;
            this.channel = channel;
            this.waveform = waveform.waveform();
            this.counter = new AtomicLong(0);
            this.runIndefinitely = runIndefinitely;
        }

        @Override
        public void run() {
            var currentRun = counter.getAndIncrement();
            var cursor = currentRun;
            var cycle = waveform.size();
            if (runIndefinitely) {
                if (currentRun >= cycle) {
                    cursor = currentRun - cycle * (currentRun / cycle);
                }
            }

            if (currentRun >= maxRun && !runIndefinitely) {
                switch (channel) {
                    case A -> device.coyote.updateWaveformA(INITIAL_FREQ, INITIAL_STRENGTH);
                    case B -> device.coyote.updateWaveformB(INITIAL_FREQ, INITIAL_STRENGTH);
                }

                var scheduledFuture = device.activeTasks.get(id);
                assert scheduledFuture != null;
                device.activeTasks.remove(id);
                scheduledFuture.cancel(true);

                return;
            }

            var waveformPiece = waveform.get((int) cursor);
            if (waveformPiece != null) {
                switch (channel) {
                    case A -> device.coyote.updateWaveformA(
                            waveformPiece.getFirst(), waveformPiece.getSecond()
                    );
                    case B -> device.coyote.updateWaveformB(
                            waveformPiece.getFirst(), waveformPiece.getSecond()
                    );
                }
            }
        }
    }
}
