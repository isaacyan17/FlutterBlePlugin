# FlutterBlePlugin

 Flutter版低功耗蓝牙插件，支持Android和iOS。

## Introduction

FlutterBlePlugin 基于[flutter_blue](!https://pub.flutter-io.cn/packages/flutter_blue)做功能修改和稳定性修复。在避免API改动所带来的业务修改负担的前提下，对原有功能进行调整，以及新功能的开发，主要改动为：

* Android native code重构

* 增加读取Rssi功能

* `device.connect`方法增加retry的设置参数

* 针对部分Android机型，HID模式下blutooth LE设备`connect`方法带来的连接问题进行修改

* native `scan & connect`增加权限判断

* 增加`scan`, `connect`状态类型

* 修复MacOS M1芯片在`java protobuf`库上带来的编译问题

* 修复iOS native `getConnectedDevices`的异常问题

## Setup

### Android 

#### Android最低编译版本为21

```dart
    defaultConfig {
        minSdkVersion 21
    }
```

#### 在**android/app/src/main/AndroidManifest.xml**设置蓝牙权限

```xml 
	 <uses-permission android:name="android.permission.BLUETOOTH" />  
	 <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />  
	 <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>  
     <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
 <application
```

### iOS

#### 在**ios/Runner/Info.plist**添加权限声明

```dart 
	<dict>  
	    <key>NSBluetoothAlwaysUsageDescription</key>  
	    <string>Need BLE permission</string>  
	    <key>NSBluetoothPeripheralUsageDescription</key>  
	    <string>Need BLE permission</string>  
	    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>  
	    <string>Need Location permission</string>  
	    <key>NSLocationAlwaysUsageDescription</key>  
	    <string>Need Location permission</string>  
	    <key>NSLocationWhenInUseUsageDescription</key>  
	    <string>Need Location permission</string>
```

## Usage

### 实例化插件

```dart
FlutterBle _flutterBlePlugin = FlutterBle();
```

### 搜索LE设备

```dart
  
    List<ScanResult> r = _flutterBlePlugin.startScan()// 返回类型是一个list
	Stream r =_flutterBlePlugin.scan({
                                    List<Guid> services = const [],  //service filter collection
                                    Duration? timeout,  
                                    bool allowDuplicates = false, // allow duplicate results when scan
                                  })      // 返回类型是Stream

```

### 连接蓝牙

```dart
    device.connect() // device -> BluetoothDevice
	
	/// connect 参数，默认重连一次
	Future<void> connect({
    Duration? timeout,
    bool autoConnect = false,
    int reconnectCount = 1,
  })
```

### 断开蓝牙连接

```dart
    device.disconnect() // device -> BluetoothDevice
```

### 蓝牙状态监听

```dart
    await device.connect(); // device -> BluetoothDevice
	device.state.listen(...); //在connect方法之后监听，可以避免收到默认值
```

### 发现服务

```dart
   device.discoverServices();
```

### 广播数据

```dart
   ScanResult.advertisementData;
```

### 读取Rssi

```dart
   device.getDeviceRssi()；
```

### 读写特征值

```dart
// Reads all characteristics
var characteristics = service.characteristics;
for(BluetoothCharacteristic c in characteristics) {
    List<int> value = await c.read();
    print(value);
}

// Writes to a characteristic
await c.write([0x12, 0x34])
```

### 设置读通道
```dart
await characteristic.setNotifyValue(true);
characteristic.value.listen((value) {
    // do something with new value
});
```

### 读写MTU


```dart
///android only
final mtu = await device.mtu.first;
await device.requestMtu(512);
```

### 蓝牙开关监听


```dart
_flutterBlePlugin.state.listen()
```


## Maintain Plan

### 后续计划