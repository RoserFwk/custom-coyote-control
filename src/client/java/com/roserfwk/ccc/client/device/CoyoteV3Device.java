package com.roserfwk.ccc.client.device;

import java.util.concurrent.*;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import com.roserfwk.ccc.client.CustomCoyoteControlClient;
import moe.prwk.btleplug4j.Characteristic.Property;
import moe.prwk.btleplug4j.Characteristic;
import moe.prwk.btleplug4j.Peripheral;
import moe.prwk.btleplug4j.Service;
import moe.prwk.btleplug4j.ValueNotification;

public class CoyoteV3Device implements AutoCloseable {
    public static final String NAME = "47L121000";

    private static final String SVC_CONTROL = "0000180c-0000-1000-8000-00805f9b34fb";
    private static final String CHR_WRITE = "0000150a-0000-1000-8000-00805f9b34fb";
    private static final String CHR_NOTIFY = "0000150b-0000-1000-8000-00805f9b34fb";
    private static final String SVC_BATTERY = "0000180a-0000-1000-8000-00805f9b34fb";
    private static final String CHR_BATTERY = "00001500-0000-1000-8000-00805f9b34fb";

    private final Peripheral peripheral;
    private Characteristic writeChar;
    private Characteristic notifyChar;
    private Characteristic batteryChar;

    private int sequenceNo = 1; // 1~15
    private volatile boolean isInputAllowedA = true;
    private volatile boolean isInputAllowedB = true;

    private int pendingStrengthA = -1;
    private int pendingStrengthB = -1;

    private int actualStrengthA = 0;
    private int actualStrengthB = 0;

    private final int[] waveFreqA = new int[]{10, 10, 10, 10};
    private final int[] waveStrA = new int[]{0, 0, 0, 0};
    private final int[] waveFreqB = new int[]{10, 10, 10, 10};
    private final int[] waveStrB = new int[]{0, 0, 0, 0};

    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();

    public CoyoteV3Device(Peripheral peripheral) {
        this.peripheral = peripheral;
    }

    public CompletableFuture<Void> initialize() {
        return peripheral.connect()
                .thenCompose(v -> peripheral.discoverServices())
                .thenCompose(v -> {
                    for (Service s : peripheral.getServices()) {
                        if (s.uuid.equalsIgnoreCase(SVC_CONTROL)) {
                            for (Characteristic c : s.getCharacteristics()) {
                                if (c.uuid.equalsIgnoreCase(CHR_WRITE) && c.hasProperty(Property.WRITE_WITHOUT_RESPONSE)) {
                                    this.writeChar = c;
                                } else if (c.uuid.equalsIgnoreCase(CHR_NOTIFY) && c.hasProperty(Property.NOTIFY)) {
                                    this.notifyChar = c;
                                }
                            }
                        } else if (s.uuid.equalsIgnoreCase(SVC_BATTERY)) {
                            for (Characteristic c : s.getCharacteristics()) {
                                if (c.uuid.equalsIgnoreCase(CHR_BATTERY)) {
                                    this.batteryChar = c;
                                }
                            }
                        }
                    }

                    if (writeChar == null || notifyChar == null) {
                        throw new RuntimeException("Missing required Coyote GATT characteristics!");
                    }

                    return setupNotificationStream();
                })
                .thenRun(() -> ticker.scheduleAtFixedRate(this::tick100ms, 100, 100, TimeUnit.MILLISECONDS));
    }

    private CompletableFuture<Void> setupNotificationStream() {
        peripheral.notifications().subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ValueNotification packet) {
                if (packet.uuid().equalsIgnoreCase(CHR_NOTIFY)) {
                    byte[] data = packet.data();
                    if (data.length >= 4 && data[0] == (byte) 0xB1) {
                        handleB1Response(data);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                CustomCoyoteControlClient.LOGGER.error("Device disconnected or stream error", throwable);
            }

            @Override
            public void onComplete() {
                CustomCoyoteControlClient.LOGGER.info("Notification stream completed.");
            }
        });

        return peripheral.subscribe(notifyChar);
    }

    private void handleB1Response(byte[] data) {
        int returnSeq = data[1] & 0xFF;
        int returnStrA = data[2] & 0xFF;
        int returnStrB = data[3] & 0xFF;

        actualStrengthA = returnStrA;
        actualStrengthB = returnStrB;

        if (returnSeq == sequenceNo) {
            isInputAllowedA = true;
            isInputAllowedB = true;
        }
    }

    public void setStrengthA(int targetStrength) {
        this.pendingStrengthA = Math.clamp(targetStrength, 0, 200);
    }

    public void setStrengthB(int targetStrength) {
        this.pendingStrengthB = Math.clamp(targetStrength, 0, 200);
    }

    public void updateWaveformA(int[] freq, int[] str) {
        if (freq.length == 4 && str.length == 4) {
            System.arraycopy(freq, 0, waveFreqA, 0, 4);
            System.arraycopy(str, 0, waveStrA, 0, 4);
        }
    }

    public void updateWaveformB(int[] freq, int[] str) {
        if (freq.length == 4 && str.length == 4) {
            System.arraycopy(freq, 0, waveFreqB, 0, 4);
            System.arraycopy(str, 0, waveStrB, 0, 4);
        }
    }

    public CompletableFuture<Void> setParameters(int softLimitA, int softLimitB, int freqBalA, int freqBalB, int strBalA, int strBalB) {
        byte[] bfPayload = new byte[]{
                (byte) 0xBF,
                (byte) softLimitA, (byte) softLimitB,
                (byte) freqBalA, (byte) freqBalB,
                (byte) strBalA, (byte) strBalB
        };
        return peripheral.writeValue(writeChar, bfPayload, true);
    }

    public CompletableFuture<Byte> getBattery() {
        return peripheral.readValue(batteryChar)
                .thenApply(bytes -> bytes[0]);
    }

    private void tick100ms() {
        if (!peripheral.isConnected().join()) return;

        byte[] b0 = new byte[20];
        b0[0] = (byte) 0xB0;

        int currentOrderNo = 0;
        int parseA = 0b00;
        int parseB = 0b00;
        int setStrA = 0;
        int setStrB = 0;

        boolean strengthChanged = false;

        if (isInputAllowedA && pendingStrengthA != -1 && pendingStrengthA != actualStrengthA) {
            parseA = 0b11;
            setStrA = pendingStrengthA;
            pendingStrengthA = -1;
            isInputAllowedA = false;
            strengthChanged = true;
        }

        if (isInputAllowedB && pendingStrengthB != -1 && pendingStrengthB != actualStrengthB) {
            parseB = 0b11;
            setStrB = pendingStrengthB;
            pendingStrengthB = -1;
            isInputAllowedB = false;
            strengthChanged = true;
        }

        if (strengthChanged) {
            sequenceNo = (sequenceNo % 15) + 1;
            currentOrderNo = sequenceNo;
        }

        b0[1] = (byte) ((currentOrderNo << 4) | (parseA << 2) | parseB);
        b0[2] = (byte) setStrA;
        b0[3] = (byte) setStrB;

        for (int i = 0; i < 4; i++) b0[4 + i] = (byte) mapFreq(waveFreqA[i]);
        for (int i = 0; i < 4; i++) b0[8 + i] = (byte) waveStrA[i];

        for (int i = 0; i < 4; i++) b0[12 + i] = (byte) mapFreq(waveFreqB[i]);
        for (int i = 0; i < 4; i++) b0[16 + i] = (byte) waveStrB[i];

        peripheral.writeValue(writeChar, b0, true).exceptionally(ex -> {
            CustomCoyoteControlClient.LOGGER.error("B0 Write Failed", ex);
            return null;
        });
    }

    private int mapFreq(int input) {
        if (input >= 10 && input <= 100) return input;
        if (input >= 101 && input <= 600) return (input - 100) / 5 + 100;
        if (input >= 601 && input <= 1000) return (input - 600) / 10 + 200;
        return 10;
    }

    @Override
    public void close() {
        ticker.shutdownNow();
        peripheral.close();
    }
}
