import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';

/// Shown once, right after a new user's first login.
class NameScreen extends StatefulWidget {
  const NameScreen({super.key, required this.auth});

  final AuthStore auth;

  @override
  State<NameScreen> createState() => _NameScreenState();
}

class _NameScreenState extends State<NameScreen> {
  final _name = TextEditingController();
  final _password = TextEditingController();
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _name.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final name = _name.text.trim();
    if (name.isEmpty) {
      setState(() => _error = 'Enter your name');
      return;
    }
    final password = _password.text;
    if (password.isNotEmpty && password.length < 8) {
      setState(() => _error = 'Password: use at least 8 characters, or leave it empty');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      // Password first: saving the name moves the app on to Home.
      if (password.isNotEmpty) await widget.auth.setPassword(newPassword: password);
      await widget.auth.saveName(name);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
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
            padding: EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('Welcome to BookDhara!', style: textTheme.titleLarge),
                  SizedBox(height: 8),
                  Text(
                    'What should buyers and sellers call you?',
                    style: TextStyle(color: context.pal.muted),
                  ),
                  SizedBox(height: 24),
                  TextField(
                    controller: _name,
                    autofocus: true,
                    textCapitalization: TextCapitalization.words,
                    autofillHints: [AutofillHints.name],
                    maxLength: 80,
                    onSubmitted: (_) => _save(),
                    decoration: InputDecoration(labelText: 'Your name', counterText: ''),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _password,
                    obscureText: true,
                    autofillHints: const [AutofillHints.newPassword],
                    onSubmitted: (_) => _save(),
                    decoration: InputDecoration(
                      labelText: 'Create a password (optional)',
                      helperText: 'Log in faster next time, without an email code',
                      errorText: _error,
                      errorMaxLines: 2,
                    ),
                  ),
                  SizedBox(height: 20),
                  BusyButton(label: 'Continue', busy: _busy, onPressed: _save),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
