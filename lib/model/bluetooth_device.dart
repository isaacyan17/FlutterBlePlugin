// Copyright 2017, Paul DeMarco.
// All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of flutter_ble;

class BluetoothDevice {
  final DeviceIdentifier id;
  final String name;
  final BluetoothDeviceType type;

  BluetoothDevice.fromProto(protos.BluetoothDevice p)
      : id = DeviceIdentifier(p.remoteId),
        name = p.name,
        type = BluetoothDeviceType.values[p.type.value];

  BehaviorSubject<bool> _isDiscoveringServices = BehaviorSubject.seeded(false);

  Stream<bool> get isDiscoveringServices => _isDiscoveringServices.stream;

  /// Establishes a connection to the Bluetooth Device.
  Future<void> connect({
    Duration? timeout,
    bool autoConnect = false,
    int reconnectCount = 1,
  }) async {
    var request = protos.ConnectRequest.create()
      ..remoteId = id.toString()
      ..androidAutoConnect = autoConnect
      ..reconncectCount = reconnectCount;

    Timer? timer;
    if (timeout != null) {
      timer = Timer(timeout, () {
        disconnect();
        throw TimeoutException('Failed to connect in time.', timeout);
      });
    }

    await FlutterBlePlatform.instance
        .invoke('connect', request.writeToBuffer());

    await state.firstWhere((s) => s == BluetoothDeviceState.connected);

    timer?.cancel();

    return;
  }

  /// Cancels connection to the Bluetooth Device
  Future disconnect() =>
      FlutterBlePlatform.instance.invoke('disconnect', id.toString());

  BehaviorSubject<List<BluetoothService>> _services =
      BehaviorSubject.seeded([]);

  /// Discovers services offered by the remote device as well as their characteristics and descriptors
  Future<List<BluetoothService>> discoverServices() async {
    final s = await state.first;
    if (s != BluetoothDeviceState.connected) {
      return Future.error(Exception(
          'Cannot discoverServices while device is not connected. State == $s'));
    }
    var response = FlutterBlePlatform.instance._methodStream
        .where((m) => m.method == "DiscoverServicesResult")
        .map((m) => m.arguments)
        .map((buffer) => protos.DiscoverServicesResult.fromBuffer(buffer))
        .where((p) => p.remoteId == id.toString())
        .map((p) => p.services)
        .map((s) => s.map((p) => BluetoothService.fromProto(p)).toList())
        .first
        .then((list) {
      _services.add(list);
      _isDiscoveringServices.add(false);
      return list;
    });

    await FlutterBlePlatform.instance.invoke('discoverServices', id.toString());

    _isDiscoveringServices.add(true);

    return response;
  }

  /// Returns a list of Bluetooth GATT services offered by the remote device
  /// This function requires that discoverServices has been completed for this device
  Stream<List<BluetoothService>> get services async* {
    yield await FlutterBlePlatform.instance
        .invoke('services', id.toString())
        .then((buffer) =>
            protos.DiscoverServicesResult.fromBuffer(buffer).services)
        .then((i) => i.map((s) => BluetoothService.fromProto(s)).toList());
    yield* _services.stream;
  }

  /// The current connection state of the device
  Stream<BluetoothDeviceState> get state async* {
    yield await FlutterBlePlatform.instance
        .invoke('deviceState', id.toString())
        .then((buffer) => protos.DeviceStateResponse.fromBuffer(buffer))
        .then((p) => BluetoothDeviceState.values[p.state.value]);

    yield* FlutterBlePlatform.instance._methodStream
        .where((m) => m.method == "DeviceState")
        .map((m) => m.arguments)
        .map((buffer) => protos.DeviceStateResponse.fromBuffer(buffer))
        .where((p) => p.remoteId == id.toString())
        .map((p) {
      return BluetoothDeviceState.values[p.state.value];
    });
  }

  Future<BluetoothDeviceState> deviceState() async{
    return await FlutterBlePlatform.instance
        .invoke('deviceState', id.toString())
        .then((buffer) => protos.DeviceStateResponse.fromBuffer(buffer))
        .then((p) => BluetoothDeviceState.values[p.state.value]);
  }

  /// The MTU size in bytes
  Stream<int> get mtu async* {
    yield await FlutterBlePlatform.instance
        .invoke('mtu', id.toString())
        .then((buffer) => protos.MtuSizeResponse.fromBuffer(buffer))
        .then((p) => p.mtu);

    yield* FlutterBlePlatform.instance._methodStream
        .where((m) => m.method == "MtuSize")
        .map((m) => m.arguments)
        .map((buffer) => protos.MtuSizeResponse.fromBuffer(buffer))
        .where((p) => p.remoteId == id.toString())
        .map((p) => p.mtu);
  }

  /// Request to change the MTU Size
  /// Throws error if request did not complete successfully
  Future<void> requestMtu(int desiredMtu) async {
    var request = protos.MtuSizeRequest.create()
      ..remoteId = id.toString()
      ..mtu = desiredMtu;

    return FlutterBlePlatform.instance
        .invoke('requestMtu', request.writeToBuffer());
  }

  /// Indicates whether the Bluetooth Device can send a write without response
  Future<bool> get canSendWriteWithoutResponse =>
      Future.error(UnimplementedError());


  ///读取设备rssi值
  Future<DeviceRssi> getDeviceRssi() async {
    await FlutterBlePlatform.instance.invoke('readRssi', id.toString());
    return FlutterBlePlatform.instance._methodStream
        .where((m) => m.method == 'DeviceRssi')
        .map((m) => m.arguments)
        .map((arg) => protos.DeviceRssi.fromBuffer(arg))
        .where((event) => event.remoteId == id.toString())
        .map((buffer) {
          return DeviceRssi.fromProto(buffer);
      }).first;
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is BluetoothDevice &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;

  @override
  String toString() {
    return 'BluetoothDevice{id: $id, name: $name, type: $type, isDiscoveringServices: ${_isDiscoveringServices.value}, _services: ${_services.value}';
  }
}

enum BluetoothDeviceType { unknown, classic, le, dual }

enum BluetoothDeviceState { disconnected, connecting, connected, disconnecting }

class DeviceIdentifier {
  final String id;

  const DeviceIdentifier(this.id);

  @override
  String toString() => id;

  @override
  int get hashCode => id.hashCode;

  @override
  bool operator ==(other) =>
      other is DeviceIdentifier && compareAsciiLowerCase(id, other.id) == 0;
}
