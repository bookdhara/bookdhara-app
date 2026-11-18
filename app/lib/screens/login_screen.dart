import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../auth/email.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';
import 'otp_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, required this.auth});

  final AuthStore auth;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _email = TextEditingController();
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _email.dispose();
    super.dispose();
  }

  Future<void> _sendOtp() async {
    final error = validateEmail(_email.text);
    setState(() => _error = error);
    if (error != null) return;

    setState(() => _busy = true);
    try {
      final sent = await widget.auth.requestOtp(_email.text);
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
                  Text('Log in with your email', style: textTheme.titleLarge),
                  const SizedBox(height: 8),
                  const Text(
                    "We'll email you a 6-digit code to verify it's you.",
                    style: TextStyle(color: AppColors.muted),
                  ),
                  const SizedBox(height: 24),
                  TextField(
                    controller: _email,
                    keyboardType: TextInputType.emailAddress,
                    autofillHints: const [AutofillHints.email],
                    autocorrect: false,
                    textInputAction: TextInputAction.go,
                    onSubmitted: (_) => _sendOtp(),
                    decoration: InputDecoration(
                      labelText: 'Email',
                      hintText: 'you@example.com',
                      errorText: _error,
                    ),
                  ),
                  const SizedBox(height: 20),
                  BusyButton(label: 'Send code', busy: _busy, onPressed: _sendOtp),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
