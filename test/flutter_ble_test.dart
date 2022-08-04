import 'package:flutter_ble/flutter_ble.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterBlePlatform
    with MockPlatformInterfaceMixin
     {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<int?> getState() {
    // TODO: implement getState
    throw UnimplementedError();
  }
}

void main() {
  final FlutterBlePlatform initialPlatform = FlutterBlePlatform.instance;

  test('$MethodChannelFlutterBle is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterBle>());
  });

  test('getPlatformVersion', () async {
    FlutterBle flutterBlePlugin = FlutterBle();
    MockFlutterBlePlatform fakePlatform = MockFlutterBlePlatform();
    // FlutterBlePlatform.instance = fakePlatform;

  });
}
