import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';

class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, required this.auth, required this.sent});

  final AuthStore auth;
  final OtpSent sent;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _otp = TextEditingController();
  late OtpSent _sent = widget.sent;
  String? _error;
  bool _busy = false;
  bool _resending = false;

  @override
  void dispose() {
    _otp.dispose();
    super.dispose();
  }

  Future<void> _verify() async {
    if (_otp.text.length != 6) {
      setState(() => _error = 'Enter the 6-digit code');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.auth.verifyOtp(_sent.phone, _otp.text);
      if (!mounted) return;
      // Logged in: go back to the start, where the app shows the next screen.
      Navigator.of(context).popUntil((route) => route.isFirst);
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _resend() async {
    setState(() => _resending = true);
    try {
      final sent = await widget.auth.requestOtp(_sent.phone);
      if (!mounted) return;
      setState(() {
        _sent = sent;
        _error = null;
      });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('New code sent')));
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _resending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('Enter the code', style: textTheme.titleLarge),
                  const SizedBox(height: 8),
                  Text('Sent to ${_sent.phone}', style: const TextStyle(color: AppColors.muted)),
                  if (_sent.devCode != null) ...[
                    const SizedBox(height: 16),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.accentTint,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFFF4D9A8)),
                      ),
                      child: Text(
                        'Test mode (no SMS yet): your code is ${_sent.devCode}',
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                  TextField(
                    controller: _otp,
                    autofocus: true,
                    keyboardType: TextInputType.number,
                    autofillHints: const [AutofillHints.oneTimeCode],
                    maxLength: 6,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    onSubmitted: (_) => _verify(),
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 24, letterSpacing: 12, fontWeight: FontWeight.w700),
                    decoration: InputDecoration(counterText: '', hintText: '------', errorText: _error),
                  ),
                  const SizedBox(height: 20),
                  BusyButton(label: 'Verify & continue', busy: _busy, onPressed: _verify),
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: _resending || _busy ? null : _resend,
                    child: Text(_resending ? 'Sending…' : "Didn't get it? Send again"),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
