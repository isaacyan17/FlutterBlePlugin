import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_ble/flutter_ble.dart';
import 'package:flutter_ble_example/custom_blue_button.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:rxdart/rxdart.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  BluetoothState _state = BluetoothState.unauthorized;
  final _flutterBlePlugin = FlutterBle();
  TextEditingController? controller = TextEditingController();
  ScrollController scrollController = ScrollController();
  var list = ['蓝牙状态', '搜索', '连接', '断开', '服务','rssi'];
  var textString = '';
  BluetoothDevice? device;

  @override
  void initState() {
    super.initState();
  }
  

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: [
            Container(
              width: 500,
              height: 300,
              margin: EdgeInsets.only(left: 10, right: 10, top: 10),
              child: TextField(
                controller: controller,
                scrollController: scrollController,
                expands: true,
                readOnly: true,
                maxLines: null,
                minLines: null,
              ),
            ),
            Container(
              height: 40,
              child: SizedBox(
                width: 80,
                child: InkWell(
                  onTap: () {
                    setState(() {
                      textString = '';
                      controller?.text = textString;
                    });
                  },
                  child: Center(
                    child: Text('清屏'),
                  ),
                ),
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                child: GridView.builder(
                    physics: NeverScrollableScrollPhysics(),
                    shrinkWrap: true,
                    gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      childAspectRatio: 1,
                    ),
                    itemCount: list.length,
                    itemBuilder: (c, i) {
                      return _button(c, i);
                    }),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _button(BuildContext context, int i) {
    return Container(
        width: 60,
        height: 44,
        margin: EdgeInsets.only(left: 10, right: 10, top: 10),
        decoration: BoxDecoration(color: Colors.lightBlueAccent),
        child: InkWell(
            onTap: () {
              switch (i) {
                case 0:
                  _flutterBlePlugin.state.listen((event) {
                    setState(() {
                      textString = '$textString\n$event';
                      controller?.text = textString;
                    });
                  });
                  break;
                case 1:
                  print('开始扫描');
                  _flutterBlePlugin
                      .startScan(timeout: Duration(seconds: 10))
                      .then((value) {
                    print('扫描完成');
                    // for(ScanResult s in value){
                    //   print(s.device.name);
                    // }
                    _flutterBlePlugin.scanResults.first.then((value) {
                      for (ScanResult s in value) {
                        if(s.device.name.isNotEmpty){
                          print(s.device.name);
                        }

                      }
                    });
                  });
                  break;
                case 2:
                  _flutterBlePlugin.scanResults.first.then((value) {
                    for (ScanResult s in value) {
                      if (s.device.name == 'D422010013') {
                        device = s.device;
                        print('连接目标设备：${s.device.name}');
                        s.device.connect(timeout: Duration(seconds: 10));
                        s.device.state.listen((event) {
                          if (event == BluetoothDeviceState.connecting) {
                            print('蓝牙连接状态: connecting');
                          }
                          if (event == BluetoothDeviceState.connected) {
                            print('蓝牙连接状态: connected');
                          }
                          if (event == BluetoothDeviceState.disconnected) {
                            print('蓝牙连接状态: disconnected');
                          }
                        });
                      }
                    }
                  });
                  break;
                case 3:
                  device?.disconnect();
                  break;
                case 4:
                  device?.services.first.then((value) {
                    for(BluetoothService s in value){
                      print(s);
                    }
                  });
                  break;
                case 5:
                  device?.getDeviceRssi().then((value) => print(value));
                  break;
              }
            },
            child: Center(child: Text(list[i]))));
  }

  void print(dynamic string) {
    setState(() {
      textString = '$textString\n$string';
      controller?.text = textString;
      // scrollController.animateTo(offset, duration: duration, curve: curve) = controller?.text.length;
    });
  }
}
