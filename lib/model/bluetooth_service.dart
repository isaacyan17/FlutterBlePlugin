

part of flutter_ble;

class BluetoothService {
  final Guid uuid;
  final DeviceIdentifier deviceId;
  final bool isPrimary;
  final List<BluetoothCharacteristic> characteristics;
  final List<BluetoothService> includedServices;

  BluetoothService.fromProto(protos.BluetoothService p)
      : uuid =  Guid(p.uuid),
        deviceId =  DeviceIdentifier(p.remoteId),
        isPrimary = p.isPrimary,
        characteristics = p.characteristics
            .map((c) =>  BluetoothCharacteristic.fromProto(c))
            .toList(),
        includedServices = p.includedServices
            .map((s) =>  BluetoothService.fromProto(s))
            .toList();

  @override
  String toString() {
    return 'BluetoothService{uuid: $uuid, deviceId: $deviceId, isPrimary: $isPrimary, characteristics: $characteristics, includedServices: $includedServices}';
  }
}
