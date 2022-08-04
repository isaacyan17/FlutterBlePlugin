part of flutter_ble;

class ScanResult {
  ScanResult.fromProto(protos.ScanResult p)
      : device =  BluetoothDevice.fromProto(p.device),
        advertisementData =
         AdvertisementData.fromProto(p.advertisementData),
        rssi = p.rssi;

  final BluetoothDevice device;
  final AdvertisementData advertisementData;
  final int rssi;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
          other is ScanResult &&
              runtimeType == other.runtimeType &&
              device == other.device;

  @override
  int get hashCode => device.hashCode;

  @override
  String toString() {
    return 'ScanResult{device: $device, advertisementData: $advertisementData, rssi: $rssi}';
  }
}
