package com.isaac.flutter_ble.controller;


import com.isaac.flutter_ble.model.BluetoothDeviceModel;
import com.isaac.flutter_ble.utils.BleLruHashMap;

import java.util.ArrayList;
import java.util.List;

public class BluetoothController {
    private final BleLruHashMap<String, BluetoothDeviceModel> bleLruHashMap;
    ///扫描时的结果缓存
    private final List<String> scanDeviceBuffer;

    public BluetoothController() {
        bleLruHashMap = new BleLruHashMap<String, BluetoothDeviceModel>(7);
        scanDeviceBuffer = new ArrayList<>();
    }

    public void addLruCacheDevice(BluetoothDeviceModel device) {
        if (device == null) {
            return;
        }
        if (!bleLruHashMap.containsKey(device.getDeviceId())) {
            bleLruHashMap.put(device.getDeviceId(), device);
        }
    }

    public void removeLruCacheDevice(BluetoothDeviceModel device) {
        if (device == null) {
            return;
        }
        if (bleLruHashMap.containsKey(device.getDeviceId())) {
            bleLruHashMap.remove(device.getDeviceId());
        }
    }

    public BluetoothDeviceModel removeLruCacheDevice(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        return bleLruHashMap.remove(deviceId);
    }


    public boolean containsLruCacheDevice(String deviceId) {
        if (deviceId != null && bleLruHashMap.containsKey(deviceId)) {
            return true;
        }
        return false;
    }

    public BluetoothDeviceModel getLruCacheDevice(String deviceId) {
        if (deviceId != null) {
            return bleLruHashMap.get(deviceId);
        }
        return null;
    }


    public void addScannedDevice(String remoteId) {
        scanDeviceBuffer.add(remoteId);
    }

    public boolean hasScannedDevice(String remoteId) {
        return scanDeviceBuffer.contains(remoteId);
    }

    public void clearScannedDevice() {
        scanDeviceBuffer.clear();
    }

}
