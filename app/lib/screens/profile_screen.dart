import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../books/books_api.dart';
import '../settings/app_settings.dart';
import '../theme.dart';
import 'info_screens.dart';
import 'my_listings_screen.dart';

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
                subtitle: const Text('Coming with buying, in the next update'),
                enabled: false,
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
