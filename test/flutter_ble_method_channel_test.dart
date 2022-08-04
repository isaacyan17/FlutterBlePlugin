import 'package:flutter/services.dart';
import 'package:flutter_ble/flutter_ble.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  MethodChannelFlutterBle platform = MethodChannelFlutterBle();
  const MethodChannel channel = MethodChannel('flutter_ble');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
