import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../books/books_api.dart';
import '../orders/orders_api.dart';
import '../settings/app_settings.dart';
import '../theme.dart';
import 'info_screens.dart';
import 'my_listings_screen.dart';
import 'my_orders_screen.dart';
import 'wallet_screen.dart';
import 'blocked_users_screen.dart';
import 'saved_books_screen.dart';
import 'book_alerts_screen.dart';

/// Asks twice (typing DELETE) because this can't be undone.
Future<void> _deleteAccount(BuildContext context, AuthStore auth) async {
  final typed = TextEditingController();
  final ok = await showDialog<bool>(
    context: context,
    builder: (ctx) => StatefulBuilder(
      builder: (ctx, setLocal) => AlertDialog(
        title: const Text('Delete your account?'),
        content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('This permanently removes your profile, your listings and photos, and any credits you have. '
              'Past orders stay for the other person as "Deleted user". You need to finish or cancel open orders first.'),
          const SizedBox(height: 12),
          const Text('Type DELETE to confirm:'),
          TextField(controller: typed, autofocus: true, onChanged: (_) => setLocal(() {})),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: ctx.pal.danger),
            onPressed: typed.text.trim() == 'DELETE' ? () => Navigator.pop(ctx, true) : null,
            child: const Text('Delete forever'),
          ),
        ],
      ),
    ),
  );
  typed.dispose();
  if (ok != true || !context.mounted) return;
  try {
    final navigator = Navigator.of(context);
    await auth.deleteAccount();
    navigator.popUntil((r) => r.isFirst);
  } on ApiException catch (e) {
    if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
  }
}

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key, required this.auth, required this.books, required this.settings});

  final AuthStore auth;
  final BooksApi books;
  final AppSettings settings;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([auth, settings]),
      builder: (context, _) {
        final user = auth.user;
        final name = user?.name ?? '';
        final textTheme = Theme.of(context).textTheme;
        return Scaffold(
          appBar: AppBar(title: const Text('Profile')),
          body: ListView(
            padding: const EdgeInsets.symmetric(vertical: 16),
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Row(children: [
                  CircleAvatar(
                    radius: 30,
                    backgroundColor: context.pal.primaryTint,
                    foregroundColor: context.pal.primary,
                    child: Text(name.isEmpty ? '?' : name.characters.first.toUpperCase(),
                        style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w700)),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(name, style: textTheme.titleLarge),
                      const SizedBox(height: 2),
                      Text(user?.email ?? '', style: TextStyle(color: context.pal.muted)),
                      if (user?.phone != null)
                        Text(user!.phone!, style: TextStyle(color: context.pal.muted)),
                    ]),
                  ),
                  IconButton(
                    tooltip: 'Edit profile',
                    icon: const Icon(Icons.edit_outlined),
                    onPressed: () => showModalBottomSheet<void>(
                      context: context,
                      isScrollControlled: true,
                      builder: (_) => _EditProfileSheet(auth: auth),
                    ),
                  ),
                ]),
              ),
              const SizedBox(height: 24),
              _Section('Your books'),
              ListTile(
                leading: const Icon(Icons.menu_book_outlined),
                title: const Text('My listings'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => MyListingsScreen(books: books),
                )),
              ),
              ListTile(
                leading: const Icon(Icons.receipt_long_outlined),
                title: const Text('My orders'),
                subtitle: const Text('Books you bought and sold'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => MyOrdersScreen(orders: OrdersApi(books.api)),
                )),
              ),
              ListTile(
                leading: const Icon(Icons.account_balance_wallet_outlined),
                title: const Text('Wallet'),
                subtitle: const Text('Credits and earnings'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => WalletScreen(orders: OrdersApi(books.api)),
                )),
              ),
              ListTile(
                leading: const Icon(Icons.favorite_border),
                title: const Text('Saved books'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => SavedBooksScreen(books: books),
                )),
              ),
              ListTile(
                leading: const Icon(Icons.notifications_none),
                title: const Text('Book alerts'),
                subtitle: const Text("Get an email when a book you want is listed"),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => BookAlertsScreen(books: books),
                )),
              ),
              const SizedBox(height: 8),
              _Section('Security'),
              ListTile(
                leading: const Icon(Icons.lock_outline),
                title: Text(user?.hasPassword == true ? 'Change password' : 'Set a password'),
                subtitle: Text(user?.hasPassword == true
                    ? 'You can log in with email + password'
                    : 'Log in faster, without an email code'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => showModalBottomSheet<void>(
                  context: context,
                  isScrollControlled: true,
                  builder: (_) => _PasswordSheet(auth: auth),
                ),
              ),
              ListTile(
                leading: const Icon(Icons.block),
                title: const Text('Blocked people'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => BlockedUsersScreen(api: books.api),
                )),
              ),
              const SizedBox(height: 8),
              _Section('Appearance'),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                child: SegmentedButton<ThemeMode>(
                  segments: const [
                    ButtonSegment(value: ThemeMode.system, label: Text('Auto'), icon: Icon(Icons.brightness_auto)),
                    ButtonSegment(value: ThemeMode.light, label: Text('Light'), icon: Icon(Icons.light_mode_outlined)),
                    ButtonSegment(value: ThemeMode.dark, label: Text('Dark'), icon: Icon(Icons.dark_mode_outlined)),
                  ],
                  selected: {settings.themeMode},
                  showSelectedIcon: false,
                  onSelectionChanged: (s) => settings.setThemeMode(s.first),
                ),
              ),
              const SizedBox(height: 8),
              _Section('BookDhara'),
              ListTile(
                leading: const Icon(Icons.info_outline),
                title: const Text('About BookDhara'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context)
                    .push(MaterialPageRoute<void>(builder: (_) => const AboutScreen())),
              ),
              ListTile(
                leading: const Icon(Icons.gavel_outlined),
                title: const Text('Terms & Privacy'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context)
                    .push(MaterialPageRoute<void>(builder: (_) => const TermsScreen())),
              ),
              const SizedBox(height: 16),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: OutlinedButton.icon(
                  onPressed: () {
                    Navigator.of(context).popUntil((r) => r.isFirst);
                    auth.logout();
                  },
                  icon: const Icon(Icons.logout),
                  label: const Text('Log out'),
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(48),
                    foregroundColor: context.pal.danger,
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Center(
                child: TextButton(
                  onPressed: () => _deleteAccount(context, auth),
                  style: TextButton.styleFrom(foregroundColor: context.pal.muted),
                  child: const Text('Delete my account'),
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        );
      },
    );
  }
}

class _Section extends StatelessWidget {
  const _Section(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 4),
        child: Text(text,
            style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: context.pal.muted)),
      );
}

class _EditProfileSheet extends StatefulWidget {
  const _EditProfileSheet({required this.auth});

  final AuthStore auth;

  @override
  State<_EditProfileSheet> createState() => _EditProfileSheetState();
}

class _EditProfileSheetState extends State<_EditProfileSheet> {
  late final _name = TextEditingController(text: widget.auth.user?.name ?? '');
  late final _phone = TextEditingController(
    text: (widget.auth.user?.phone ?? '').replaceFirst('+91', ''),
  );
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_name.text.trim().isEmpty) {
      setState(() => _error = 'Enter your name');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.auth.saveProfile(name: _name.text, phone: _phone.text.trim());
      if (mounted) Navigator.of(context).pop();
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 20, 20, 20 + MediaQuery.of(context).viewInsets.bottom),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text('Edit profile', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 16),
        TextField(
          controller: _name,
          textCapitalization: TextCapitalization.words,
          maxLength: 80,
          decoration: const InputDecoration(labelText: 'Name', counterText: ''),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _phone,
          keyboardType: TextInputType.phone,
          inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9\s]'))],
          decoration: const InputDecoration(
            labelText: 'Mobile number (optional)',
            prefixText: '+91  ',
            helperText: 'Needed later for home delivery orders',
          ),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(_error!, style: TextStyle(color: context.pal.danger)),
          ),
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _busy ? null : _save,
          child: _busy
              ? SizedBox(
                  width: 22,
                  height: 22,
                  child: CircularProgressIndicator(strokeWidth: 2.5, color: context.pal.onPrimary))
              : const Text('Save'),
        ),
      ]),
    );
  }
}


class _PasswordSheet extends StatefulWidget {
  const _PasswordSheet({required this.auth});

  final AuthStore auth;

  @override
  State<_PasswordSheet> createState() => _PasswordSheetState();
}

class _PasswordSheetState extends State<_PasswordSheet> {
  final _current = TextEditingController();
  final _next = TextEditingController();
  String? _error;
  bool _busy = false;

  bool get _changing => widget.auth.user?.hasPassword == true;

  @override
  void dispose() {
    _current.dispose();
    _next.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_next.text.length < 8) {
      setState(() => _error = 'Use at least 8 characters');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.auth.setPassword(current: _changing ? _current.text : null, newPassword: _next.text);
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Password saved')));
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 20, 20, 20 + MediaQuery.of(context).viewInsets.bottom),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text(_changing ? 'Change password' : 'Set a password', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 16),
        if (_changing) ...[
          TextField(
            controller: _current,
            obscureText: true,
            autofillHints: const [AutofillHints.password],
            decoration: const InputDecoration(labelText: 'Current password'),
          ),
          const SizedBox(height: 12),
        ],
        TextField(
          controller: _next,
          obscureText: true,
          autofillHints: const [AutofillHints.newPassword],
          decoration: const InputDecoration(labelText: 'New password', helperText: 'At least 8 characters'),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(_error!, style: TextStyle(color: context.pal.danger)),
          ),
        const SizedBox(height: 16),
        FilledButton(onPressed: _busy ? null : _save, child: Text(_busy ? 'Saving…' : 'Save password')),
      ]),
    );
  }
}
