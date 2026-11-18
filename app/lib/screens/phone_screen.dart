import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../auth/phone.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';
import 'otp_screen.dart';

class PhoneScreen extends StatefulWidget {
  const PhoneScreen({super.key, required this.auth});

  final AuthStore auth;

  @override
  State<PhoneScreen> createState() => _PhoneScreenState();
}

class _PhoneScreenState extends State<PhoneScreen> {
  final _phone = TextEditingController();
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _phone.dispose();
    super.dispose();
  }

  Future<void> _sendOtp() async {
    final error = validateIndianMobile(_phone.text);
    setState(() => _error = error);
    if (error != null) return;

    setState(() => _busy = true);
    try {
      final sent = await widget.auth.requestOtp(_phone.text);
      if (!mounted) return;
      await Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => OtpScreen(auth: widget.auth, sent: sent)),
      );
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('BookDhara', style: textTheme.headlineMedium?.copyWith(color: AppColors.primary)),
                  const SizedBox(height: 4),
                  const Text('Kitaabein behti rahein.', style: TextStyle(color: AppColors.muted)),
                  const SizedBox(height: 40),
                  Text('Log in with your phone', style: textTheme.titleLarge),
                  const SizedBox(height: 8),
                  const Text(
                    "We'll send a 6-digit code to verify it's you.",
                    style: TextStyle(color: AppColors.muted),
                  ),
                  const SizedBox(height: 24),
                  TextField(
                    controller: _phone,
                    keyboardType: TextInputType.phone,
                    autofillHints: const [AutofillHints.telephoneNumber],
                    inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9+\s\-]'))],
                    onSubmitted: (_) => _sendOtp(),
                    decoration: InputDecoration(
                      labelText: 'Mobile number',
                      prefixText: '+91  ',
                      errorText: _error,
                    ),
                  ),
                  const SizedBox(height: 20),
                  BusyButton(label: 'Send OTP', busy: _busy, onPressed: _sendOtp),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
