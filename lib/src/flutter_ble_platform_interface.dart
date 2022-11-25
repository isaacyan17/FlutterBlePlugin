part of flutter_ble;

abstract class FlutterBlePlatform extends PlatformInterface {
  /// Constructs a FlutterBlePlatform.
  FlutterBlePlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterBlePlatform _instance = MethodChannelFlutterBle();

  /// The default instance of [FlutterBlePlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterBle].
  static FlutterBlePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterBlePlatform] when
  /// they register themselves.
  static set instance(FlutterBlePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Stream<BluetoothState> get state {
    throw UnimplementedError('getState() has not been implemented.');
  }

  Future startScan({
    List<Guid> services = const [],
    Duration? timeout,
    bool allowDuplicates = false,
  }) {
    throw UnimplementedError('getState() has not been implemented.');
  }

  Stream<ScanResult> scan({
    List<Guid> services = const [],
    Duration? timeout,
    bool allowDuplicates = false,
  }) {
    throw UnimplementedError('getState() has not been implemented.');
  }

  Stream<List<ScanResult>> get scanResults {
    throw UnimplementedError('getState() has not been implemented.');
  }
  Stream<MethodCall> get _methodStream {
    throw UnimplementedError('getState() has not been implemented.');
  }

  Future<T?> invoke<T>(String method, [ dynamic arguments ]){
    throw UnimplementedError('getState() has not been implemented.');
  }

 Future stopScan(){
   throw UnimplementedError('getState() has not been implemented.');
 }

  /// Checks whether the device supports Bluetooth
  Future<bool> get isAvailable => throw UnimplementedError('getState() has not been implemented.');

  /// Checks if Bluetooth functionality is turned on
  Future<bool> get isOn => throw UnimplementedError('getState() has not been implemented.');

  Stream<bool> get isScanning => throw UnimplementedError('getState() has not been implemented.');

  Future<List<BluetoothDevice>> get connectedDevices => throw UnimplementedError('getState() has not been implemented.');


}
