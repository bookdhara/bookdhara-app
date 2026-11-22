import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../theme.dart';
import 'safety_api.dart';

/// "⋮" menu with Report and Block, used on book pages, orders and chats.
class SafetyMenu extends StatelessWidget {
  const SafetyMenu({
    super.key,
    required this.safety,
    required this.userId,
    required this.userName,
    this.bookId,
    this.onBlocked,
  });

  final SafetyApi safety;
  final int userId;
  final String userName;

  /// When set, "Report listing" is offered too.
  final int? bookId;
  final VoidCallback? onBlocked;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<String>(
      tooltip: 'More',
      onSelected: (v) {
        switch (v) {
          case 'report-book':
            showReportSheet(context, safety, ReportTarget.book, bookId!, 'this listing');
          case 'report-user':
            showReportSheet(context, safety, ReportTarget.user, userId, userName);
          case 'block':
            confirmBlock(context, safety, userId, userName, onBlocked);
        }
      },
      itemBuilder: (_) => [
        if (bookId != null)
          const PopupMenuItem(value: 'report-book', child: ListTile(leading: Icon(Icons.flag_outlined), title: Text('Report listing'))),
        PopupMenuItem(value: 'report-user', child: ListTile(leading: const Icon(Icons.report_outlined), title: Text('Report $userName'))),
        PopupMenuItem(value: 'block', child: ListTile(leading: const Icon(Icons.block), title: Text('Block $userName'))),
      ],
    );
  }
}

Future<void> showReportSheet(BuildContext context, SafetyApi safety, ReportTarget target, int id, String what) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _ReportSheet(safety: safety, target: target, id: id, what: what),
  );
}

Future<void> confirmBlock(BuildContext context, SafetyApi safety, int userId, String name, VoidCallback? onBlocked) async {
  final ok = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text('Block $name?'),
      content: Text("You won't see each other's books, and neither of you can buy from or message the other. "
          'Orders you already have together stay, so you can still finish or cancel them. You can unblock from Profile.'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: ctx.pal.danger),
          onPressed: () => Navigator.pop(ctx, true),
          child: const Text('Block'),
        ),
      ],
    ),
  );
  if (ok != true || !context.mounted) return;
  try {
    await safety.block(userId);
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$name is blocked.')));
    onBlocked?.call();
  } on ApiException catch (e) {
    if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
  }
}

class _ReportSheet extends StatefulWidget {
  const _ReportSheet({required this.safety, required this.target, required this.id, required this.what});

  final SafetyApi safety;
  final ReportTarget target;
  final int id;
  final String what;

  @override
  State<_ReportSheet> createState() => _ReportSheetState();
}

class _ReportSheetState extends State<_ReportSheet> {
  final _details = TextEditingController();
  ReportReason? _reason;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _details.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final reason = _reason;
    if (reason == null) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.safety.report(widget.target, widget.id, reason, _details.text);
      if (!mounted) return;
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Thanks. We'll look into it. You can also block them from the same menu.")),
      );
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
            Text('Report ${widget.what}', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 4),
            Text('Reports are private. The other person is not told who reported them.',
                style: TextStyle(color: context.pal.muted)),
            const SizedBox(height: 12),
            for (final r in ReportReason.forTarget(widget.target))
              ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: Icon(
                  _reason == r ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                  color: _reason == r ? context.pal.primary : context.pal.muted,
                ),
                title: Text(r.label),
                onTap: () => setState(() => _reason = r),
              ),
            const SizedBox(height: 8),
            TextField(
              controller: _details,
              maxLength: 500,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'What happened? (optional)'),
            ),
            if (_error != null) Text(_error!, style: TextStyle(color: context.pal.danger)),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _reason == null || _busy ? null : _submit,
                child: _busy
                    ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Send report'),
              ),
            ),
          ]),
        ),
      ),
    );
  }
}
