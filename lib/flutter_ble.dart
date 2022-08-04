library flutter_ble;

import 'dart:async';
import 'package:flutter/widgets.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import '../gen/flutterble.pb.dart' as protos;
import 'package:collection/collection.dart';
import 'package:convert/convert.dart';
import 'package:rxdart/rxdart.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

part 'model/bluetooth_state.dart';

part 'src/flutter_ble.dart';

part 'src/flutter_ble_method_channel.dart';

part 'src/flutter_ble_platform_interface.dart';

part 'src/guid.dart';

part 'model/scan_result.dart';

part 'model/bluetooth_device.dart';

part 'model/advertisement.dart';

part 'model/bluetooth_service.dart';

part 'model/bluetooth_characteristic.dart';

part 'model/bluetooth_descriptor.dart';
part 'model/rssi.dart';
