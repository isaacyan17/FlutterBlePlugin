part of flutter_ble;

class FlutterBle {
  FlutterBle._();

  static final FlutterBle _instance = FlutterBle._();

  factory FlutterBle() => _instance;

  /// 手机蓝牙状态
  Stream<BluetoothState> get state => FlutterBlePlatform.instance.state;

  Future startScan({
    List<Guid> services = const [],
    Duration? timeout,
    bool allowDuplicates = false,
  }) {
    return FlutterBlePlatform.instance.startScan(
        services: services, timeout: timeout, allowDuplicates: allowDuplicates);
  }

  Future stopScan() {
    return FlutterBlePlatform.instance.stopScan();
  }

  Stream<ScanResult> scan({
    List<Guid> services = const [],
    Duration? timeout,
    bool allowDuplicates = false,
  }) {
    return FlutterBlePlatform.instance.scan(
        services: services, timeout: timeout, allowDuplicates: allowDuplicates);
  }

  Stream<List<ScanResult>> get scanResults =>
      FlutterBlePlatform.instance.scanResults;

  Future<bool> get isAvailable => FlutterBlePlatform.instance.isAvailable;

  Future<bool> get isOn => FlutterBlePlatform.instance.isOn;

  Stream<bool> get isScanning => FlutterBlePlatform.instance.isScanning;

  Future<List<BluetoothDevice>> get connectedDevices =>
      FlutterBlePlatform.instance.connectedDevices;
}
