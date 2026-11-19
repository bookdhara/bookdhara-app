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
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final name = _name.text.trim();
    if (name.isEmpty) {
      setState(() => _error = 'Enter your name');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
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
                    decoration: InputDecoration(labelText: 'Your name', counterText: '', errorText: _error),
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
