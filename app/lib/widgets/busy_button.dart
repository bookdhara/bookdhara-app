import 'dart:async';

import 'package:flutter/material.dart';

import '../theme.dart';

/// A main action button that shows a spinner while busy. If the wait passes a few seconds,
/// it explains that the free server may be waking up.
class BusyButton extends StatefulWidget {
  const BusyButton({super.key, required this.label, required this.busy, required this.onPressed});

  final String label;
  final bool busy;
  final VoidCallback? onPressed;

  @override
  State<BusyButton> createState() => _BusyButtonState();
}

class _BusyButtonState extends State<BusyButton> {
  Timer? _timer;
  bool _slow = false;

  @override
  void didUpdateWidget(BusyButton oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.busy && !oldWidget.busy) {
      _timer = Timer(Duration(seconds: 5), () {
        if (mounted) setState(() => _slow = true);
      });
    } else if (!widget.busy && oldWidget.busy) {
      _timer?.cancel();
      _slow = false;
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        FilledButton(
          onPressed: widget.busy ? null : widget.onPressed,
          child: widget.busy
              ? SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(strokeWidth: 2.5, color: context.pal.onPrimary),
                )
              : Text(widget.label),
        ),
        if (_slow) ...[
          SizedBox(height: 10),
          Text(
            'Waking up the server. The first try can take up to a minute, please wait.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13, color: context.pal.muted),
          ),
        ],
      ],
    );
  }
}
