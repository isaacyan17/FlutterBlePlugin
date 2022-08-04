part of flutter_ble;

class DeviceRssi {
  final String remoteId;
  final int rssi;

  @override
  String toString() {
    return 'DeviceRssi{remoteId: $remoteId, rssi: $rssi}';
  }

  DeviceRssi.fromProto(protos.DeviceRssi p)
      : remoteId = p.remoteId,
        rssi = p.rssi;

}
