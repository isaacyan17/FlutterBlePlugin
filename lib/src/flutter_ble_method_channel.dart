part of flutter_ble;

/// An implementation of [FlutterBlePlatform] that uses method channels.
class MethodChannelFlutterBle extends FlutterBlePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_ble');
  final eventChannel = const EventChannel('flutter_ble/event');

  ///rxdart的api说明：https://pub.dev/documentation/rxdart/latest/rx/BehaviorSubject-class.html
  BehaviorSubject<bool> _isScanning = BehaviorSubject.seeded(false);

  @override
  Stream<bool> get isScanning => _isScanning.stream;

  ///停止扫描的订阅
  final PublishSubject<dynamic> _stopScanStream = PublishSubject();

  ///扫描结果
  final BehaviorSubject<List<ScanResult>> _scanResults =
      BehaviorSubject.seeded([]);

  @override
  Stream<List<ScanResult>> get scanResults => _scanResults.stream;

  /// method回调controller
  final StreamController<MethodCall> _methodController =
      StreamController.broadcast();

  @override
  Stream<MethodCall> get _methodStream => _methodController.stream;

  ///构造函数
  MethodChannelFlutterBle() {
    methodChannel
        .setMethodCallHandler((call) async {
          // print('-============setMethodCallHandler, ${call.method}');
      _methodController.add(call);
    });
  }

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    print(version);
    return version;
  }

  @override
  Stream<BluetoothState> get state async* {
    yield await methodChannel
        .invokeMethod('state')
        .then((value) => protos.BluetoothState.fromBuffer(value))
        .then((v) => BluetoothState.values[v.state.value]);
    yield* eventChannel
        .receiveBroadcastStream()
        .map((event) => protos.BluetoothState.fromBuffer(event))
        .map((event) => BluetoothState.values[event.state.value]);
  }

  @override
  Future startScan({
    List<Guid> services = const [],
    Duration? timeout,
    bool allowDuplicates = false,
  }) async {
    await scan(services: services, timeout: timeout,allowDuplicates: allowDuplicates).drain();
    return _scanResults.value;
  }

  @override
  Stream<ScanResult> scan({
    List<Guid> services = const [],
    Duration? timeout,
    bool allowDuplicates = false,
  }) async* {
    var settings = protos.ScanSettings.create()
      ..allowDuplicates = allowDuplicates
      ..serviceUuids.addAll(services.map((e) => e.toString()).toList());
    if (_isScanning.value == true) {
      throw Exception(
          "scan action already exists, complete the previous scan action first");
    }
    _isScanning.add(true);
    var mergeStream = <Stream>[];
    mergeStream.add(_stopScanStream);
    if (timeout != null) {
      mergeStream.add(Rx.timer(null, timeout));
    }

    _scanResults.add([]);

    try {
      await methodChannel.invokeMethod('startScan', settings.writeToBuffer());
    } catch (e) {
      ///恢复状态
      _stopScanStream.add(null);
      _isScanning.add(false);

      ///抛出异常，可以被Future.onError捕获
      rethrow;
    }

    /// takeUntil: https://pub.dev/documentation/rxdart/latest/rx/TakeUntilExtension/takeUntil.html
    /// 当其他流产生数据后，原stream停止返回数据。

    yield* _methodStream
        .where((m) {
          return m.method == 'ScanResult';
        })
        .map((event) => event.arguments)
        .takeUntil(Rx.merge(mergeStream))
        .doOnDone(stopScan)
        .map((buffer) => protos.ScanResult.fromBuffer(buffer))
        .map((p) {
        // print(event);
      final result = ScanResult.fromProto(p);
      final list = _scanResults.value ;
      int index = list.indexOf(result);
      if (index != -1) {
        list[index] = result;
      } else {
        list.add(result);
      }
      _scanResults.add(list);
      return result;
    });
  }

  ///停止扫描
  @override
  Future stopScan() async {
    try {
      print('停止扫描');
      await methodChannel.invokeMethod('stopScan');
    } finally{
      _stopScanStream.add(null);
      _isScanning.add(false);
    }
  }

  @override
  Future<T?> invoke<T>(String method, [arguments]) {
    return methodChannel.invokeMethod(method,arguments);
  }

  /// Checks whether the device supports Bluetooth
  @override
  Future<bool> get isAvailable =>
      methodChannel.invokeMethod('isAvailable').then<bool>((d) => d);

  /// Checks if Bluetooth functionality is turned on
  @override
  Future<bool> get isOn => methodChannel.invokeMethod('isOn').then<bool>((d) => d);

  /// Retrieve a list of connected devices
  @override
  Future<List<BluetoothDevice>> get connectedDevices {
    return methodChannel
        .invokeMethod('getConnectedDevices')
        .then((buffer) => protos.ConnectedDevicesResponse.fromBuffer(buffer))
        .then((p) => p.devices)
        .then((p) => p.map((d) => BluetoothDevice.fromProto(d)).toList());
  }
}
