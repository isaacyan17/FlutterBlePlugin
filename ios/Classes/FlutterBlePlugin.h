#import <Flutter/Flutter.h>
#import <CoreBluetooth/CoreBluetooth.h>
#define NAMESPACE @"flutter_ble"
@interface FlutterBlePlugin : NSObject<FlutterPlugin, CBCentralManagerDelegate, CBPeripheralDelegate>
@end

@interface FlutterBleStreamHandler : NSObject<FlutterStreamHandler>
@property FlutterEventSink sink;
@end
