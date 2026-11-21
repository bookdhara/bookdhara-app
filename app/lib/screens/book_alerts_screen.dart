import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../books/books_api.dart';
import '../theme.dart';

/// "Tell me when it's listed": we email when a new book matches the title, author or ISBN.
class BookAlertsScreen extends StatefulWidget {
  const BookAlertsScreen({super.key, required this.books});

  final BooksApi books;

  @override
  State<BookAlertsScreen> createState() => _BookAlertsScreenState();
}

class _BookAlertsScreenState extends State<BookAlertsScreen> {
  final _query = TextEditingController();
  final _city = TextEditingController();
  List<BookAlert>? _alerts;
  String? _error;
  bool _adding = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _query.dispose();
    _city.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final list = await widget.books.alerts();
      if (mounted) setState(() => _alerts = list);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _add() async {
    final q = _query.text.trim();
    if (q.length < 3) return;
    setState(() => _adding = true);
    try {
      final city = _city.text.trim();
      final alert = await widget.books.addAlert(q, city: city.isEmpty ? null : city);
      if (!mounted) return;
      setState(() {
        _alerts = [alert, ...?_alerts];
        _query.clear();
        _city.clear();
      });
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _adding = false);
    }
  }

  Future<void> _remove(BookAlert a) async {
    try {
      await widget.books.removeAlert(a.id);
      if (mounted) setState(() => _alerts = _alerts?.where((x) => x.id != a.id).toList());
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final alerts = _alerts;
    return Scaffold(
      appBar: AppBar(title: const Text('Book alerts')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text("Tell us a title, author or ISBN. When someone lists a match, we'll email you.",
              style: TextStyle(color: context.pal.muted)),
          const SizedBox(height: 16),
          TextField(
            controller: _query,
            onChanged: (_) => setState(() {}),
            textInputAction: TextInputAction.next,
            decoration: const InputDecoration(labelText: 'Title, author or ISBN'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _city,
            onSubmitted: (_) => _add(),
            decoration: const InputDecoration(labelText: 'City (optional)', hintText: 'Any city'),
          ),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: _adding || _query.text.trim().length < 3 ? null : _add,
            icon: const Icon(Icons.notifications_active_outlined),
            label: const Text('Add alert'),
          ),
          const SizedBox(height: 24),
          if (_error != null) Text(_error!),
          if (alerts == null && _error == null) const Center(child: CircularProgressIndicator()),
          if (alerts != null && alerts.isEmpty)
            Text('No alerts yet.', style: TextStyle(color: context.pal.muted)),
          if (alerts != null)
            for (final a in alerts)
              Card(
                child: ListTile(
                  leading: const Icon(Icons.notifications_none),
                  title: Text(a.query),
                  subtitle: Text(a.city ?? 'Any city'),
                  trailing: IconButton(tooltip: 'Remove', icon: const Icon(Icons.delete_outline), onPressed: () => _remove(a)),
                ),
              ),
        ],
      ),
    );
  }
}
