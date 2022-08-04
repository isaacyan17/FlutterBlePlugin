package com.isaac.flutter_ble.model;

public class BleRuleConfig {
    private String remoteId;
    private int reConnectCount = 1;
    private boolean autoConnect = false;

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public int getReConnectCount() {
        return reConnectCount;
    }

    public void setReConnectCount(int reConnectCount) {
        this.reConnectCount = reConnectCount;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }
}
