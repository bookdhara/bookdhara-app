import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../safety/safety_api.dart';
import '../theme.dart';

/// People I've blocked, with Unblock.
class BlockedUsersScreen extends StatefulWidget {
  const BlockedUsersScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<BlockedUsersScreen> createState() => _BlockedUsersScreenState();
}

class _BlockedUsersScreenState extends State<BlockedUsersScreen> {
  late final SafetyApi _safety = SafetyApi(widget.api);
  List<BlockedUser>? _list;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = await _safety.blocked();
      if (mounted) setState(() => _list = list);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _unblock(BlockedUser u) async {
    try {
      await _safety.unblock(u.id);
      if (!mounted) return;
      setState(() => _list = _list?.where((x) => x.id != u.id).toList());
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${u.name ?? 'They'} can see your books again.')));
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final list = _list;
    return Scaffold(
      appBar: AppBar(title: const Text('Blocked people')),
      body: _error != null
          ? Center(child: Text(_error!))
          : list == null
              ? const Center(child: CircularProgressIndicator())
              : list.isEmpty
                  ? Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32),
                        child: Text("You haven't blocked anyone.", style: TextStyle(color: context.pal.muted)),
                      ),
                    )
                  : ListView(children: [
                      for (final u in list)
                        ListTile(
                          leading: const Icon(Icons.person_off_outlined),
                          title: Text(u.name ?? 'BookDhara user'),
                          trailing: TextButton(onPressed: () => _unblock(u), child: const Text('Unblock')),
                        ),
                    ]),
    );
  }
}
