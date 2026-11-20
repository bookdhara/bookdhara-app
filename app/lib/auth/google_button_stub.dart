import 'package:flutter/widgets.dart';

/// Not supported on this platform yet; shows nothing.
class GoogleSignInButton extends StatelessWidget {
  const GoogleSignInButton({super.key, required this.clientId, required this.onCredential});

  final String clientId;
  final void Function(String credential) onCredential;

  static const bool supported = false;

  @override
  Widget build(BuildContext context) => const SizedBox.shrink();
}
