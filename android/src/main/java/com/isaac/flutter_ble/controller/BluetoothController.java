package com.isaac.flutter_ble.controller;

import android.bluetooth.BluetoothGattCallback;

import com.isaac.flutter_ble.model.BluetoothDeviceModel;
import com.isaac.flutter_ble.utils.BleLruHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BluetoothController {
    private final BleLruHashMap<String, BluetoothDeviceModel> bleLruHashMap;

    public BluetoothController() {
        bleLruHashMap = new BleLruHashMap<String, BluetoothDeviceModel>(7);
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

    public void removeLruCacheDevice(String deviceId) {
        if (deviceId == null) {
            return;
        }
        bleLruHashMap.remove(deviceId);
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

    public void setLruCacheDeviceGattCallback(BluetoothDeviceModel device) {
        if (device != null) {
            bleLruHashMap.put(device.getDeviceId(), device);
        }
    }

    public BluetoothGattCallback getLruCacheDeviceGattCallback(String remoteId) {
        if (remoteId != null) {
            BluetoothDeviceModel bluetoothDeviceModel = bleLruHashMap.get(remoteId);
            if (bluetoothDeviceModel != null)
                return bluetoothDeviceModel.getGattCallback();
        }
        return null;
    }

}
