package com.isaac.flutter_ble;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.isaac.flutter_ble.controller.BluetoothController;
import com.isaac.flutter_ble.model.BleRuleConfig;
import com.isaac.flutter_ble.model.BluetoothDeviceModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * FlutterBlePlugin
 */
public class FlutterBlePlugin implements FlutterPlugin, ActivityAware,MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private static final String tag = "FlutterBle";
    static final private UUID CCCD_ID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //方法通道
    private MethodChannel channel;
    //事件流通道
    private EventChannel eventChannel;
    private Activity mActivity;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothController mBleController;
    private Context mAppContext;
    private MethodCall pendingCall;
    private Result pendingResult;
    //
    private ScanCallback scanCallback21;
    private BluetoothAdapter.LeScanCallback scanCallback18;
    ///扫描结果是否允许重复值
    private boolean allowDuplicates = false;

    private BleRuleConfig ruleConfig;

//    public static void registerWith(PluginRegistry.Registrar registrar) {
//
//    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        System.out.println("-----------------onAttachedToEngine");
        mAppContext = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_ble");
        channel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_ble/event");
        eventChannel.setStreamHandler(eventHandler);
        setup();

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
//        System.out.println("--------------------------activity: " + (mActivity == null));
        if (call.method.equals("state")) {
            // 设备蓝牙状态
            Protos.BluetoothState.Builder b = Protos.BluetoothState.newBuilder();
            try {
                switch (mBluetoothAdapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        b.setState(Protos.BluetoothState.State.ON);
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        b.setState(Protos.BluetoothState.State.OFF);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        b.setState(Protos.BluetoothState.State.TURNING_ON);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        b.setState(Protos.BluetoothState.State.TURNING_OFF);
                        break;
                }
            } catch (SecurityException e) {
                b.setState(Protos.BluetoothState.State.UNAUTHORIZED);
            } finally {
                System.out.println("--------method -state");
                result.success(b.build().toByteArray());
            }
        } else if (call.method.equals("isOn")) {
            //蓝牙是否开启
            result.success(isBlueEnable());
        } else if (call.method.equals("isAvailable")) {
            //蓝牙适配器是否可用
            result.success(mBluetoothAdapter != null);
        } else if (call.method.equals("startScan")) {
            //扫描蓝牙设备
            startScan(call, result);
            result.success(null);
        } else if (call.method.equals("stopScan")) {
            //停止扫描蓝牙设备
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopScan21();
            } else {
                stopScan18();
            }
            result.success(null);
        } else if (call.method.equals("getConnectedDevices")) {
            List<BluetoothDevice> devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            Protos.ConnectedDevicesResponse.Builder p = Protos.ConnectedDevicesResponse.newBuilder();
            for (BluetoothDevice d : devices) {
                p.addDevices(ProtoMaker.from(d));
            }
            result.success(p.build().toByteArray());
        } else if (call.method.equals("connect")) {
            pendingCall = call;
            pendingResult = result;
            try {
                byte[] arguments = pendingCall.arguments();
                Protos.ConnectRequest b = Protos.ConnectRequest.newBuilder().mergeFrom(arguments).build();
                ruleConfig.setAutoConnect(b.getAndroidAutoConnect());
                ruleConfig.setReConnectCount(b.getReconncectCount());
                ruleConfig.setRemoteId(b.getRemoteId());

                connect();
                pendingResult.success(null);
            } catch (InvalidProtocolBufferException e) {
                result.error("RuntimeException", e.getMessage(), e);
                e.printStackTrace();
            }

        }else if (call.method.equals("disconnect")) {
            String deviceId = (String) call.arguments;
            disConnect(deviceId);
            result.success(null);
        } else if (call.method.equals("deviceState")) {
            String deviceId = (String) call.arguments;
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceId);
            int state = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
            try {
                result.success(ProtoMaker.from(device, state).toByteArray());
            } catch (Exception e) {
                result.error("device_state_error", e.getMessage(), e);
            }

        } else if (call.method.equals("discoverServices")) {
            String deviceId = (String)call.arguments;
            try {
                BluetoothGatt gatt = mBleController.getLruCacheDevice(deviceId).getGatt();

                if(gatt.discoverServices()) {
                    result.success(null);
                } else {
                    result.error("discover_services_error", "unknown reason", null);
                }
            } catch(Exception e) {
                result.error("discover_services_error", e.getMessage(), e);
            }
        }else if (call.method.equals("services")) {
            String deviceId = (String)call.arguments;
            try {
                BluetoothGatt gatt = mBleController.getLruCacheDevice(deviceId).getGatt();
                Protos.DiscoverServicesResult.Builder p = Protos.DiscoverServicesResult.newBuilder();
                p.setRemoteId(deviceId);
                for(BluetoothGattService s : gatt.getServices()){
                    p.addServices(ProtoMaker.from(gatt.getDevice(), s, gatt));
                }
                result.success(p.build().toByteArray());
            } catch(Exception e) {
                result.error("get_services_error", e.getMessage(), e);
            }
        }else if (call.method.equals("readCharacteristic")) {
            byte[] data = call.arguments();
            Protos.ReadCharacteristicRequest request;
            try {
                request = Protos.ReadCharacteristicRequest.newBuilder().mergeFrom(data).build();
            } catch (InvalidProtocolBufferException e) {
                result.error("RuntimeException", e.getMessage(), e);
                return;
            }

            BluetoothGatt gattServer;
            BluetoothGattCharacteristic characteristic;
            try {
                gattServer = mBleController.getLruCacheDevice(request.getRemoteId()).getGatt();
                characteristic = locateCharacteristic(gattServer, request.getServiceUuid(), request.getSecondaryServiceUuid(), request.getCharacteristicUuid());
            } catch(Exception e) {
                result.error("read_characteristic_error", e.getMessage(), null);
                return;
            }

            if(gattServer.readCharacteristic(characteristic)) {
                result.success(null);
            } else {
                result.error("read_characteristic_error", "unknown reason, may occur if readCharacteristic was called before last read finished.", null);
            }
        }else if (call.method.equals("readDescriptor")) {
            byte[] data = call.arguments();
            Protos.ReadDescriptorRequest request;
            try {
                request = Protos.ReadDescriptorRequest.newBuilder().mergeFrom(data).build();
            } catch (InvalidProtocolBufferException e) {
                result.error("RuntimeException", e.getMessage(), e);
                return;
            }

            BluetoothGatt gattServer;
            BluetoothGattCharacteristic characteristic;
            BluetoothGattDescriptor descriptor;
            try {
                gattServer = mBleController.getLruCacheDevice(request.getRemoteId()).getGatt();
                characteristic = locateCharacteristic(gattServer, request.getServiceUuid(), request.getSecondaryServiceUuid(), request.getCharacteristicUuid());
                descriptor = locateDescriptor(characteristic, request.getDescriptorUuid());
            } catch(Exception e) {
                result.error("read_descriptor_error", e.getMessage(), null);
                return;
            }

            if(gattServer.readDescriptor(descriptor)) {
                result.success(null);
            } else {
                result.error("read_descriptor_error", "unknown reason, may occur if readDescriptor was called before last read finished.", null);
            }

        }else if (call.method.equals("writeCharacteristic")) {
            byte[] data = call.arguments();
            Protos.WriteCharacteristicRequest request;
            try {
                request = Protos.WriteCharacteristicRequest.newBuilder().mergeFrom(data).build();
            } catch (InvalidProtocolBufferException e) {
                result.error("RuntimeException", e.getMessage(), e);
                return;
            }

            BluetoothGatt gattServer;
            BluetoothGattCharacteristic characteristic;
            try {
                gattServer = mBleController.getLruCacheDevice(request.getRemoteId()).getGatt();
                characteristic = locateCharacteristic(gattServer, request.getServiceUuid(), request.getSecondaryServiceUuid(), request.getCharacteristicUuid());
            } catch(Exception e) {
                result.error("write_characteristic_error", e.getMessage(), null);
                return;
            }

            // Set characteristic to new value
            if(!characteristic.setValue(request.getValue().toByteArray())){
                result.error("write_characteristic_error", "could not set the local value of characteristic", null);
            }

            // Apply the correct write type
            if(request.getWriteType() == Protos.WriteCharacteristicRequest.WriteType.WITHOUT_RESPONSE) {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            } else {
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            }

            if(!gattServer.writeCharacteristic(characteristic)){
                result.error("write_characteristic_error", "writeCharacteristic failed", null);
                return;
            }

            result.success(null);
        }else if (call.method.equals("writeDescriptor")) {
            byte[] data = call.arguments();
            Protos.WriteDescriptorRequest request;
            try {
                request = Protos.WriteDescriptorRequest.newBuilder().mergeFrom(data).build();
            } catch (InvalidProtocolBufferException e) {
                result.error("RuntimeException", e.getMessage(), e);
                return;
            }

            BluetoothGatt gattServer;
            BluetoothGattCharacteristic characteristic;
            BluetoothGattDescriptor descriptor;
            try {
                gattServer = mBleController.getLruCacheDevice(request.getRemoteId()).getGatt();
                characteristic = locateCharacteristic(gattServer, request.getServiceUuid(), request.getSecondaryServiceUuid(), request.getCharacteristicUuid());
                descriptor = locateDescriptor(characteristic, request.getDescriptorUuid());
            } catch(Exception e) {
                result.error("write_descriptor_error", e.getMessage(), null);
                return;
            }

            // Set descriptor to new value
            if(!descriptor.setValue(request.getValue().toByteArray())){
                result.error("write_descriptor_error", "could not set the local value for descriptor", null);
            }

            if(!gattServer.writeDescriptor(descriptor)){
                result.error("write_descriptor_error", "writeCharacteristic failed", null);
                return;
            }

            result.success(null);
        }else if (call.method.equals("setNotification")) {
            byte[] data = call.arguments();
            Protos.SetNotificationRequest request;
            try {
                request = Protos.SetNotificationRequest.newBuilder().mergeFrom(data).build();
            } catch (InvalidProtocolBufferException e) {
                result.error("RuntimeException", e.getMessage(), e);
                return;
            }

            BluetoothGatt gattServer;
            BluetoothGattCharacteristic characteristic;
            BluetoothGattDescriptor cccDescriptor;
            try {
                gattServer = mBleController.getLruCacheDevice(request.getRemoteId()).getGatt();
                characteristic = locateCharacteristic(gattServer, request.getServiceUuid(), request.getSecondaryServiceUuid(), request.getCharacteristicUuid());
                cccDescriptor = characteristic.getDescriptor(CCCD_ID);
                if(cccDescriptor == null) {
                    throw new Exception("could not locate CCCD descriptor for characteristic: " +characteristic.getUuid().toString());
                }
            } catch(Exception e) {
                result.error("set_notification_error", e.getMessage(), null);
                return;
            }

            byte[] value = null;

            if(request.getEnable()) {
                boolean canNotify = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0;
                boolean canIndicate = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0;
                if(!canIndicate && !canNotify) {
                    result.error("set_notification_error", "the characteristic cannot notify or indicate", null);
                    return;
                }
                if(canIndicate) {
                    value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                }
                if(canNotify) {
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                }
            } else {
                value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            }

            if(!gattServer.setCharacteristicNotification(characteristic, request.getEnable())){
                result.error("set_notification_error", "could not set characteristic notifications to :" + request.getEnable(), null);
                return;
            }

            if(!cccDescriptor.setValue(value)) {
                result.error("set_notification_error", "error when setting the descriptor value to: " + value, null);
                return;
            }

            if(!gattServer.writeDescriptor(cccDescriptor)) {
                result.error("set_notification_error", "error when writing the descriptor", null);
                return;
            }

            result.success(null);
        }else if (call.method.equals("mtu")) {
            String deviceId = (String)call.arguments;
            BluetoothDeviceModel lruCacheDevice = mBleController.getLruCacheDevice(deviceId);
            if(lruCacheDevice != null) {
                Protos.MtuSizeResponse.Builder p = Protos.MtuSizeResponse.newBuilder();
                p.setRemoteId(deviceId);
                p.setMtu(lruCacheDevice.getMtu());
                result.success(p.build().toByteArray());
            } else {
                result.error("mtu", "no instance of BluetoothGatt, have you connected first?", null);
            }
        }else if (call.method.equals("requestMtu")) {
            byte[] data = call.arguments();
            Protos.MtuSizeRequest request;
            try {
                request = Protos.MtuSizeRequest.newBuilder().mergeFrom(data).build();
            } catch (InvalidProtocolBufferException e) {
                result.error("RuntimeException", e.getMessage(), e);
                return;
            }

            BluetoothGatt gatt;
            try {
                gatt = mBleController.getLruCacheDevice(request.getRemoteId()).getGatt();
                int mtu = request.getMtu();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if(gatt.requestMtu(mtu)) {
                        result.success(null);
                    } else {
                        result.error("requestMtu", "gatt.requestMtu returned false", null);
                    }
                } else {
                    result.error("requestMtu", "Only supported on devices >= API 21 (Lollipop). This device == " + Build.VERSION.SDK_INT, null);
                }
            } catch(Exception e) {
                result.error("requestMtu", e.getMessage(), e);
            }
        } else if (call.method.equals("readRssi")) {
            String remoteId = (String) call.arguments;
            try {
                BluetoothDeviceModel lruCacheDevice = mBleController.getLruCacheDevice(remoteId);
                if (lruCacheDevice == null) {
                    result.error("readRssi", "remote device is null!", null);
                    return;
                }
                BluetoothGatt gatt = lruCacheDevice.getGatt();
                if (gatt == null) {
                    result.error("readRssi", "device rssi is null!", null);
                    return;
                }
                if (gatt.readRemoteRssi()) {
                    result.success(null);
                } else {
                    result.error("readRssi", "read remote rssi error!", null);
                }
            } catch (Exception e) {
                result.error("readRssiException", e.getMessage(), null);
                e.printStackTrace();
            }
        }

    }

    /**
     * 连接蓝牙
     */
    public void connect() {
        try {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(ruleConfig.getRemoteId());

            if (device == null) {
                pendingResult.error("connect", "device not exist", null);
                return;
            }

            if (!isBlueEnable()) {
                pendingResult.error("connect", "Bluetooth not enable!", null);
                return;
            }
            if (Looper.myLooper() == null || Looper.myLooper() != Looper.getMainLooper()) {
                System.out.println("Be careful: currentThread is not MainThread!");
            }
            boolean isConnected = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT).contains(device);
            if (isConnected && mBleController.containsLruCacheDevice(device.getAddress())) {
                pendingResult.error("already_connected", "connection with device already exists", null);
                return;
            }
            BluetoothGatt gattServer;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                gattServer = device.connectGatt(mAppContext, ruleConfig.isAutoConnect(), mGattCallback, TRANSPORT_LE);

            } else {
                gattServer = device.connectGatt(mAppContext, ruleConfig.isAutoConnect(), mGattCallback);
            }
            //将数据存入bleLruHashMap中
            BluetoothDeviceModel lruModel = new BluetoothDeviceModel(device);
            lruModel.setGatt(gattServer);

            lruModel.setDeviceState(Protos.DeviceStateResponse.BluetoothDeviceState.CONNECTING);
            mBleController.addLruCacheDevice(lruModel);

        } catch (Exception e) {
            pendingResult.error("Exception", e.getMessage(), e);
            e.printStackTrace();
        }
    }


    /**
     * 断连蓝牙
     * @param deviceId
     */
    void disConnect(String deviceId) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceId);
        BluetoothDeviceModel lruCacheDevice = mBleController.removeLruCacheDevice(deviceId);
        if (lruCacheDevice != null) {
            lruCacheDevice.getGatt().disconnect();
            //刷新device
            refreshDeviceCache(lruCacheDevice.getGatt());

            int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

            if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                lruCacheDevice.getGatt().close();
            }
        }
    }

    /**
     * 初始化
     */
    void setup() {
        mBluetoothManager = (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleController = new BluetoothController();
        ruleConfig = new BleRuleConfig();
    }

    /**
     * 事件流handler
     */
    final EventChannel.StreamHandler eventHandler = new EventChannel.StreamHandler() {
        private EventChannel.EventSink sink;

        @Override
        public void onListen(Object arguments, EventChannel.EventSink events) {
            this.sink = events;
            //注册蓝牙开关的监听
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            filter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
            mActivity.getApplication().registerReceiver(receiver, filter);
        }

        @Override
        public void onCancel(Object arguments) {
            sink = null;
            mActivity.getApplication().unregisterReceiver(receiver);
        }

        private final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//                    System.out.println("--------蓝牙实时状态，" + intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
                    switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            sink.success(Protos.BluetoothState.newBuilder().setState(Protos.BluetoothState.State.ON).build().toByteArray());
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            sink.success(Protos.BluetoothState.newBuilder().setState(Protos.BluetoothState.State.OFF).build().toByteArray());
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            sink.success(Protos.BluetoothState.newBuilder().setState(Protos.BluetoothState.State.TURNING_ON).build().toByteArray());
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            sink.success(Protos.BluetoothState.newBuilder().setState(Protos.BluetoothState.State.TURNING_OFF).build().toByteArray());
                            break;
                    }
                }
            }
        };
    };

    /**
     * 扫描设备
     */
    public void startScan(MethodCall call,Result result) {
        //权限检查
        if (ContextCompat.checkSelfPermission(mAppContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //将权限问题抛出给宿主工程
            result.error("permission_denied", "permission access_fine_location denied", null);
            return;
        }
        byte[] data = call.arguments();
        try {
            Protos.ScanSettings b = Protos.ScanSettings.newBuilder().mergeFrom(data).build();
            allowDuplicates = b.getAllowDuplicates();
            mBleController.clearScannedDevice();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                int count  = b.getServiceUuidsCount();
                List<ScanFilter> filters = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(b.getServiceUuids(i))).build());
                }
                if (filters.size() <= 0) {
                    ///没有过滤规则的情况下，增加一个空filter,用于部分机型后台扫描
                    filters.add(new ScanFilter.Builder().build());
                }
                ScanSettings settings;
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                    settings =  new ScanSettings.Builder()
                            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                            .setLegacy(false)
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
                }else {
                    settings =  new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
                }
                bluetoothLeScanner.startScan(filters,
                        settings,
                        getScanCallback21());
            } else {
                List<String> serviceUuids = b.getServiceUuidsList();
                UUID[] uuids = new UUID[serviceUuids.size()];
                for (int i = 0; i < serviceUuids.size(); i++) {
                    uuids[i] = UUID.fromString(serviceUuids.get(i));
                }
                boolean success = mBluetoothAdapter.startLeScan(uuids, getScanCallback18());
                if (!success)
                    throw new IllegalStateException("getBluetoothLeScanner() is null. Is the Adapter on?");
            }
        } catch (Exception e) {
            result.error("start_scan_error", e.getMessage(), e);
        }
    }

    public ScanCallback getScanCallback21() {
        if (scanCallback21 == null) {
            scanCallback21 = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (result != null) {
                        if (!allowDuplicates
                                && result.getDevice() != null
                                && result.getDevice().getAddress() != null){
                            if (mBleController.hasScannedDevice(result.getDevice().getAddress())) {
                                return;
                            }
                            mBleController.addScannedDevice(result.getDevice().getAddress());
                        }

                        Protos.ScanResult scanResult = ProtoMaker.from(result.getDevice(), result);
                        invokeUIThread("ScanResult", scanResult.toByteArray());
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    System.out.println("--------------onScanFailed");

                }
            };
        }
        return scanCallback21;
    }

    private void stopScan21() {
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(getScanCallback21());
    }

    private void stopScan18() {
        mBluetoothAdapter.stopLeScan(getScanCallback18());
    }

    private BluetoothAdapter.LeScanCallback getScanCallback18() {
        if (scanCallback18 == null) {
            scanCallback18 = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi,
                                     byte[] scanRecord) {
                    if (!allowDuplicates
                            && bluetoothDevice != null
                            && bluetoothDevice.getAddress() != null){
                        if (mBleController.hasScannedDevice(bluetoothDevice.getAddress())) {
                            return;
                        }
                        mBleController.addScannedDevice(bluetoothDevice.getAddress());
                    }

                    Protos.ScanResult scanResult = ProtoMaker.from(bluetoothDevice, scanRecord, rssi);
                    invokeUIThread("ScanResult", scanResult.toByteArray());
                }
            };
        }
        return scanCallback18;
    }

    /**
     * BluetoothGattCallback
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            System.out.println("[onConnectionStateChange] status: " + status + " newState: " + newState);
            if (status == 133) {
                if (mBleController.containsLruCacheDevice(gatt.getDevice().getAddress())) {
                    if (mBleController.getLruCacheDevice(gatt.getDevice().getAddress()).getDeviceState()
                            == Protos.DeviceStateResponse.BluetoothDeviceState.CONNECTING) {

                        //如果设备之前的状态是connecting,表示是正在连接中的设备。
                        if (ruleConfig.getReConnectCount() - 1 >= 0) {
                            System.out.println("重连一次,并在重连之前删除lru缓存");
                            mBleController.removeLruCacheDevice(gatt.getDevice().getAddress());
                            ruleConfig.setReConnectCount(ruleConfig.getReConnectCount() - 1);
                            gatt.disconnect();
                            gatt.close();
                            connect();
                        }
                    }
                }
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (!mBleController.containsLruCacheDevice(gatt.getDevice().getAddress())) {
                    gatt.close();
                }else{
                    ///处理设备端主动断开蓝牙的情况,更新本地缓存数据
                    disConnect(gatt.getDevice().getAddress());
                }
            }
            invokeUIThread("DeviceState", ProtoMaker.from(gatt.getDevice(), newState).toByteArray());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            System.out.println("[onServicesDiscovered] count: " + gatt.getServices().size() + " status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mBleController.containsLruCacheDevice(gatt.getDevice().getAddress())) {
                    BluetoothDeviceModel lruCacheDevice = mBleController.getLruCacheDevice(gatt.getDevice().getAddress());
                    lruCacheDevice.setGatt(gatt);
                    lruCacheDevice.setDeviceState(Protos.DeviceStateResponse.BluetoothDeviceState.CONNECTED);
                    mBleController.addLruCacheDevice(lruCacheDevice);
                }
            }
            Protos.DiscoverServicesResult.Builder p = Protos.DiscoverServicesResult.newBuilder();
            p.setRemoteId(gatt.getDevice().getAddress());
            for(BluetoothGattService s : gatt.getServices()) {
                p.addServices(ProtoMaker.from(gatt.getDevice(), s, gatt));
            }
            invokeUIThread("DiscoverServicesResult", p.build().toByteArray());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("[onCharacteristicRead] uuid: " + characteristic.getUuid().toString() + " status: " + status);
            Protos.ReadCharacteristicResponse.Builder p = Protos.ReadCharacteristicResponse.newBuilder();
            p.setRemoteId(gatt.getDevice().getAddress());
            p.setCharacteristic(ProtoMaker.from(gatt.getDevice(), characteristic, gatt));
            invokeUIThread("ReadCharacteristicResponse", p.build().toByteArray());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("[onCharacteristicWrite] uuid: " + characteristic.getUuid().toString() + " status: " + status);
            Protos.WriteCharacteristicRequest.Builder request = Protos.WriteCharacteristicRequest.newBuilder();
            request.setRemoteId(gatt.getDevice().getAddress());
            request.setCharacteristicUuid(characteristic.getUuid().toString());
            request.setServiceUuid(characteristic.getService().getUuid().toString());
            Protos.WriteCharacteristicResponse.Builder p = Protos.WriteCharacteristicResponse.newBuilder();
            p.setRequest(request);
            p.setSuccess(status == BluetoothGatt.GATT_SUCCESS);
            invokeUIThread("WriteCharacteristicResponse", p.build().toByteArray());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            System.out.println("[onCharacteristicChanged] uuid: " + characteristic.getUuid().toString());
            ///log
            byte[] value = characteristic.getValue();
            StringBuilder sb = new StringBuilder();
            for (byte b : value) {
                sb.append(String.format("%02X", b));
            }
            System.out.println("[onCharacteristicChanged] value-----------> :: "+sb.toString());

            Protos.OnCharacteristicChanged.Builder p = Protos.OnCharacteristicChanged.newBuilder();
            p.setRemoteId(gatt.getDevice().getAddress());
            p.setCharacteristic(ProtoMaker.from(gatt.getDevice(), characteristic, gatt));
            invokeUIThread("OnCharacteristicChanged", p.build().toByteArray());
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            System.out.println("[onReadRemoteRssi] rssi: " + rssi + " status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Protos.DeviceRssi.Builder builder = Protos.DeviceRssi.newBuilder();
                builder.setRssi(rssi);
                builder.setRemoteId(gatt.getDevice().getAddress());
                invokeUIThread("DeviceRssi", builder.build().toByteArray());
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            System.out.println( "[onMtuChanged] mtu: " + mtu + " status: " + status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if(mBleController.containsLruCacheDevice(gatt.getDevice().getAddress())) {
                    BluetoothDeviceModel lruCacheDevice = mBleController.getLruCacheDevice(gatt.getDevice().getAddress());
                    lruCacheDevice.setMtu(mtu);
                    Protos.MtuSizeResponse.Builder p = Protos.MtuSizeResponse.newBuilder();
                    p.setRemoteId(gatt.getDevice().getAddress());
                    p.setMtu(mtu);
                    invokeUIThread("MtuSize", p.build().toByteArray());
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            System.out.println("[onDescriptorWrite] uuid: " + descriptor.getUuid().toString() + " status: " + status);
            Protos.WriteDescriptorRequest.Builder request = Protos.WriteDescriptorRequest.newBuilder();
            request.setRemoteId(gatt.getDevice().getAddress());
            request.setDescriptorUuid(descriptor.getUuid().toString());
            request.setCharacteristicUuid(descriptor.getCharacteristic().getUuid().toString());
            request.setServiceUuid(descriptor.getCharacteristic().getService().getUuid().toString());
            Protos.WriteDescriptorResponse.Builder p = Protos.WriteDescriptorResponse.newBuilder();
            p.setRequest(request);
            p.setSuccess(status == BluetoothGatt.GATT_SUCCESS);
            invokeUIThread("WriteDescriptorResponse", p.build().toByteArray());

            if(descriptor.getUuid().compareTo(CCCD_ID) == 0) {
                // SetNotificationResponse
                Protos.SetNotificationResponse.Builder q = Protos.SetNotificationResponse.newBuilder();
                q.setRemoteId(gatt.getDevice().getAddress());
                q.setCharacteristic(ProtoMaker.from(gatt.getDevice(), descriptor.getCharacteristic(), gatt));
                invokeUIThread("SetNotificationResponse", q.build().toByteArray());
            }
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
        }
    };

    /**
     * judge Bluetooth is enable
     *
     * @return
     */
    public boolean isBlueEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private BluetoothGattCharacteristic locateCharacteristic(BluetoothGatt gattServer, String serviceId, String secondaryServiceId, String characteristicId) throws Exception {
        BluetoothGattService primaryService = gattServer.getService(UUID.fromString(serviceId));
        if(primaryService == null) {
            throw new Exception("service (" + serviceId + ") could not be located on the device");
        }
        BluetoothGattService secondaryService = null;
        if(secondaryServiceId.length() > 0) {
            for(BluetoothGattService s : primaryService.getIncludedServices()){
                if(s.getUuid().equals(UUID.fromString(secondaryServiceId))){
                    secondaryService = s;
                }
            }
            if(secondaryService == null) {
                throw new Exception("secondary service (" + secondaryServiceId + ") could not be located on the device");
            }
        }
        BluetoothGattService service = (secondaryService != null) ? secondaryService : primaryService;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicId));
        if(characteristic == null) {
            throw new Exception("characteristic (" + characteristicId + ") could not be located in the service ("+service.getUuid().toString()+")");
        }
        return characteristic;
    }


    private BluetoothGattDescriptor locateDescriptor(BluetoothGattCharacteristic characteristic, String descriptorId) throws Exception {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorId));
        if(descriptor == null) {
            throw new Exception("descriptor (" + descriptorId + ") could not be located in the characteristic ("+characteristic.getUuid().toString()+")");
        }
        return descriptor;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        System.out.println("-----------------onAttachedToActivity");
        mActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        System.out.println("-----------------onDetachedFromActivityForConfigChanges");
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        System.out.println("-----------------onReattachedToActivityForConfigChanges");
        mActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        mActivity = null;
    }

    public void invokeUIThread(final String name, final byte[] byteArray){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                channel.invokeMethod(name,byteArray);
            }
        });
    }

    private synchronized void refreshDeviceCache(BluetoothGatt bluetoothGatt) {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null && bluetoothGatt != null) {
                boolean success = (Boolean) refresh.invoke(bluetoothGatt);
                Log.i(tag,"refreshDeviceCache, is success:  " + success);
            }
        } catch (Exception e) {
            Log.i(tag,"exception occur while refreshing device: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
