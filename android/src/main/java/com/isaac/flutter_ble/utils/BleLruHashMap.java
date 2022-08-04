package com.isaac.flutter_ble.utils;

import com.isaac.flutter_ble.model.BluetoothDeviceModel;

import java.util.LinkedHashMap;

public class BleLruHashMap<K,V> extends LinkedHashMap<K,V> {
    private final int MAX_SIZE;

    public BleLruHashMap(int maxSize){
        super((int) Math.ceil(maxSize / 0.75) + 1, 0.75f, true);
        this.MAX_SIZE = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
        if (size() > MAX_SIZE && eldest.getValue() instanceof BluetoothDeviceModel) {
            ((BluetoothDeviceModel) eldest.getValue()).disconnect();
        }
        return size() > MAX_SIZE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
