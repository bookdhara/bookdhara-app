import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../auth/email.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';

/// Forgot password: we email a code; the code plus a new password logs you in.
class ResetPasswordScreen extends StatefulWidget {
  const ResetPasswordScreen({super.key, required this.auth, this.email = ''});

  final AuthStore auth;
  final String email;

  @override
  State<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends State<ResetPasswordScreen> {
  late final _email = TextEditingController(text: widget.email);
  final _code = TextEditingController();
  final _password = TextEditingController();
  OtpSent? _sent;
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    final error = validateEmail(_email.text);
    setState(() => _error = error);
    if (error != null) return;
    setState(() => _busy = true);
    try {
      final sent = await widget.auth.requestOtp(_email.text);
      setState(() => _sent = sent);
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _save() async {
    if (_code.text.length != 6) {
      setState(() => _error = 'Enter the 6-digit code');
      return;
    }
    if (_password.text.length < 8) {
      setState(() => _error = 'Use at least 8 characters');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.auth.resetPassword(_sent!.email, _code.text, _password.text);
      if (mounted) Navigator.of(context).popUntil((r) => r.isFirst);
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final sent = _sent;
    return Scaffold(
      appBar: AppBar(title: const Text('Reset password')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              Text(
                sent == null
                    ? "We'll email you a code. Then choose a new password."
                    : 'We sent a code to ${sent.email}. Check Spam or Promotions if you can\'t find it.',
                style: TextStyle(color: context.pal.muted),
              ),
              if (sent?.devCode != null) ...[
                const SizedBox(height: 12),
                Text('Test mode: your code is ${sent!.devCode}', style: const TextStyle(fontWeight: FontWeight.w600)),
              ],
              const SizedBox(height: 20),
              TextField(
                controller: _email,
                enabled: sent == null,
                keyboardType: TextInputType.emailAddress,
                autocorrect: false,
                decoration: const InputDecoration(labelText: 'Email'),
              ),
              if (sent != null) ...[
                const SizedBox(height: 12),
                TextField(
                  controller: _code,
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  autofillHints: const [AutofillHints.oneTimeCode],
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  decoration: const InputDecoration(labelText: '6-digit code', counterText: ''),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _password,
                  obscureText: true,
                  autofillHints: const [AutofillHints.newPassword],
                  decoration: const InputDecoration(labelText: 'New password', helperText: 'At least 8 characters'),
                ),
              ],
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Text(_error!, style: TextStyle(color: context.pal.danger)),
                ),
              const SizedBox(height: 20),
              BusyButton(
                label: sent == null ? 'Send code' : 'Save password & log in',
                busy: _busy,
                onPressed: sent == null ? _sendCode : _save,
              ),
            ]),
          ),
        ),
      ),
    );
  }
}
