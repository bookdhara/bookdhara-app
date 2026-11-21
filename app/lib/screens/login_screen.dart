import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../auth/email.dart';
import '../auth/google_button.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';
import 'otp_screen.dart';
import 'reset_password_screen.dart';

/// Log in with email + password, or get a one-time code by email (also how new users sign up).
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, required this.auth});

  final AuthStore auth;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _usePassword = false;
  bool _showPassword = false;
  String? _error;
  bool _busy = false;
  String? _googleClientId;
  bool _googleBusy = false;

  @override
  void initState() {
    super.initState();
    if (GoogleSignInButton.supported) _loadGoogle();
  }

  /// The public Google client id baked into the app, so the button never waits for the server
  /// (which can take a minute to wake up). The server can still switch Google sign-in off.
  static const _builtInGoogleClientId = String.fromEnvironment(
    'GOOGLE_CLIENT_ID',
    defaultValue: '833380295837-2agen7raucf5d6mrqu0i88ckc5dllsgh.apps.googleusercontent.com',
  );

  /// Shows the Google button immediately, then asks the server whether it's still on.
  Future<void> _loadGoogle() async {
    final cached = await widget.auth.cachedGoogleClientId();
    if (!mounted) return;
    setState(() => _googleClientId = cached ?? _builtInGoogleClientId);
    try {
      final id = await widget.auth.googleClientId();
      if (mounted) setState(() => _googleClientId = id);
    } catch (_) {
      // Server unreachable or still waking: keep the button; signing in will wait for the server.
    }
  }

  Future<void> _google(String credential) async {
    setState(() {
      _googleBusy = true;
      _error = null;
    });
    try {
      await widget.auth.loginWithGoogle(credential);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'Google sign-in failed. Please try again.');
    } finally {
      if (mounted) setState(() => _googleBusy = false);
    }
  }

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final error = validateEmail(_email.text);
    setState(() => _error = error);
    if (error != null) return;
    if (_usePassword && _password.text.isEmpty) {
      setState(() => _error = 'Enter your password');
      return;
    }

    setState(() => _busy = true);
    try {
      if (_usePassword) {
        await widget.auth.loginWithPassword(_email.text, _password.text);
      } else {
        final sent = await widget.auth.requestOtp(_email.text);
        if (!mounted) return;
        await Navigator.of(context).push(
          MaterialPageRoute<void>(builder: (_) => OtpScreen(auth: widget.auth, sent: sent)),
        );
      }
    } on ApiException catch (e) {
      setState(() => _error = e.code == 'WRONG_CREDENTIALS'
          ? 'Wrong email or password. No password yet? Use "Get a code by email".'
          : e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _switchMode() => setState(() {
        _usePassword = !_usePassword;
        _error = null;
      });

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
              child: AutofillGroup(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('BookDhara', style: textTheme.headlineMedium?.copyWith(color: context.pal.primary)),
                    const SizedBox(height: 4),
                    Text('Kitaabein behti rahein.', style: TextStyle(color: context.pal.muted)),
                    const SizedBox(height: 40),
                    if (_googleClientId != null) ...[
                      if (_googleBusy)
                        const SizedBox(height: 48, child: Center(child: CircularProgressIndicator()))
                      else
                        GoogleSignInButton(clientId: _googleClientId!, onCredential: _google),
                      const SizedBox(height: 20),
                      Row(children: [
                        const Expanded(child: Divider()),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          child: Text('or use email', style: TextStyle(color: context.pal.muted)),
                        ),
                        const Expanded(child: Divider()),
                      ]),
                      const SizedBox(height: 20),
                    ],
                    Text(_usePassword ? 'Log in' : 'Log in with your email', style: textTheme.titleLarge),
                    const SizedBox(height: 8),
                    Text(
                      _usePassword
                          ? 'Use the password you set in BookDhara.'
                          : "We'll email you a 6-digit code to verify it's you. New here? This creates your account.",
                      style: TextStyle(color: context.pal.muted),
                    ),
                    const SizedBox(height: 24),
                    TextField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      autofillHints: const [AutofillHints.email, AutofillHints.username],
                      autocorrect: false,
                      textInputAction: _usePassword ? TextInputAction.next : TextInputAction.go,
                      onSubmitted: (_) => _usePassword ? null : _submit(),
                      decoration: InputDecoration(
                        labelText: 'Email',
                        hintText: 'you@example.com',
                        errorText: _usePassword ? null : _error,
                      ),
                    ),
                    if (_usePassword) ...[
                      const SizedBox(height: 12),
                      TextField(
                        controller: _password,
                        obscureText: !_showPassword,
                        autofillHints: const [AutofillHints.password],
                        autocorrect: false,
                        enableSuggestions: false,
                        textInputAction: TextInputAction.go,
                        onSubmitted: (_) => _submit(),
                        decoration: InputDecoration(
                          labelText: 'Password',
                          errorText: _error,
                          errorMaxLines: 3,
                          suffixIcon: IconButton(
                            tooltip: _showPassword ? 'Hide password' : 'Show password',
                            icon: Icon(_showPassword ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                            onPressed: () => setState(() => _showPassword = !_showPassword),
                          ),
                        ),
                      ),
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton(
                          onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(
                            builder: (_) => ResetPasswordScreen(auth: widget.auth, email: _email.text.trim()),
                          )),
                          child: const Text('Forgot password?'),
                        ),
                      ),
                    ],
                    const SizedBox(height: 20),
                    BusyButton(label: _usePassword ? 'Log in' : 'Send code', busy: _busy, onPressed: _submit),
                    const SizedBox(height: 12),
                    TextButton(
                      onPressed: _busy ? null : _switchMode,
                      child: Text(_usePassword ? 'Get a code by email instead' : 'I have a password'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
