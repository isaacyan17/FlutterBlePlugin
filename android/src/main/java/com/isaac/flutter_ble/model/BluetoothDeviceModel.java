package com.isaac.flutter_ble.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import com.isaac.flutter_ble.Protos;

public class BluetoothDeviceModel {
    int mtu;
    BluetoothGatt gatt;
    BluetoothDevice device;
    Protos.DeviceStateResponse.BluetoothDeviceState deviceState;
    private int rssi;
    BluetoothGattCallback gattCallback;

    public BluetoothDeviceModel(BluetoothDevice device) {
        this.device = device;
        this.mtu = 20;
        deviceState = Protos.DeviceStateResponse.BluetoothDeviceState.DISCONNECTED;
    }

    public BluetoothDeviceModel(BluetoothDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
        this.mtu = 20;
        deviceState = Protos.DeviceStateResponse.BluetoothDeviceState.DISCONNECTED;
    }

    public synchronized void disconnect() {
        disconnectGatt();
    }

    private synchronized void disconnectGatt() {
        if (gatt != null) {
            gatt.disconnect();
        }
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public Protos.DeviceStateResponse.BluetoothDeviceState getDeviceState() {
        return deviceState;
    }

    public void setDeviceState(Protos.DeviceStateResponse.BluetoothDeviceState deviceState) {
        this.deviceState = deviceState;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public BluetoothGattCallback getGattCallback() {
        return gattCallback;
    }

    public void setGattCallback(BluetoothGattCallback gattCallback) {
        this.gattCallback = gattCallback;
    }

    /**
     * 返回设备MAC地址
     *
     */
    public String getDeviceId() {
        if (device != null) {
            return device.getAddress();
        }
        return "";
    }
}
