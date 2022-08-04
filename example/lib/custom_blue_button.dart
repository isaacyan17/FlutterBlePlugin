import 'package:flutter/material.dart';
class CustomBlueButton extends StatefulWidget {
  CustomBlueButton({
    Key? key,
    required this.onPressed,
    required this.textString,
    this.enable = true,
  }) : super(key: key);

  VoidCallback onPressed;
  String textString;
  bool? enable;

  @override
  State<CustomBlueButton> createState() => _CustomBlueButtonState();
}

class _CustomBlueButtonState extends State<CustomBlueButton> {
  @override
  Widget build(BuildContext context) {
    return Container(
      height: 44,
      width: 80,
      // color: Colors.red,
      child: _CustomBlueTextButton(
        onPressed: widget.onPressed,
        textString: widget.textString,
        enable: widget.enable!,
      ),
    );
  }
}

class _CustomBlueTextButton extends TextButton {
  _CustomBlueTextButton({
    Key? key,
    required VoidCallback? onPressed,
    required String textString,
    required bool enable,
  }) : super(
          key: key,
          onPressed: enable ? onPressed : null,
          child: Text(textString),
          style: ButtonStyle(
            //设置按钮上字体与图标的颜色
            //foregroundColor: MaterialStateProperty.all(Colors.deepPurple),
            //更优美的方式来设置
            foregroundColor: MaterialStateProperty.resolveWith(
              (states) {
                if (states.contains(MaterialState.focused) &&
                    !states.contains(MaterialState.pressed)) {
                  //获取焦点时的颜色
                  return Colors.white;
                } else if (states.contains(MaterialState.pressed)) {
                  //按下时的颜色
                  return Colors.white;
                }
                //默认状态使用灰色
                return Colors.white;
              },
            ),
            // overlayColor: MaterialStateProperty.all(Colors.red),
            //
            minimumSize: MaterialStateProperty.all(Size(60, 26)),
          ),
        );
}
