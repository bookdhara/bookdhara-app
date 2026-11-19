import 'dart:js_interop';

import 'package:flutter/material.dart';

/// Defined in web/index.html: draws Google's official button into [element] and calls
/// [onCredential] with the signed ID token when the person picks their account.
@JS('bookdharaGoogle.render')
external void _renderGoogleButton(JSObject element, String clientId, JSFunction onCredential, String theme);

/// Google's official "Continue with Google" button (Google Identity Services).
class GoogleSignInButton extends StatelessWidget {
  const GoogleSignInButton({super.key, required this.clientId, required this.onCredential});

  final String clientId;
  final void Function(String credential) onCredential;

  static const bool supported = true;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return SizedBox(
      height: 48,
      child: HtmlElementView.fromTagName(
        // Redraw when the theme changes so the button matches.
        key: ValueKey('google-$dark'),
        tagName: 'div',
        onElementCreated: (Object element) {
          _renderGoogleButton(
            element as JSObject,
            clientId,
            ((String credential) => onCredential(credential)).toJS,
            dark ? 'filled_black' : 'outline',
          );
        },
      ),
    );
  }
}
