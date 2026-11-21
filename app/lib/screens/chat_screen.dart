import 'dart:async';

import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../orders/order_models.dart';
import '../orders/orders_api.dart';
import '../safety/safety_actions.dart';
import '../safety/safety_api.dart';
import '../theme.dart';

/// Chat between the buyer and seller of one order: agree where and when to meet without sharing
/// phone numbers. New messages are fetched every few seconds while this screen is open.
class ChatScreen extends StatefulWidget {
  const ChatScreen({
    super.key,
    required this.orders,
    required this.orderId,
    required this.iAmBuyer,
    required this.pickup,
    this.otherUserId,
  });

  final OrdersApi orders;
  final int orderId;
  final bool iAmBuyer;
  final bool pickup;
  final int? otherUserId;

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> with WidgetsBindingObserver {
  static const _pollEvery = Duration(seconds: 4);
  static final _phoneLike = RegExp(r'(\+?91[\s-]?)?[6-9]\d{4}[\s-]?\d{5}');

  final _text = TextEditingController();
  final _messages = <ChatMessage>[];
  String? _otherName;
  String? _bookTitle;
  bool _canSend = true;
  bool _loaded = false;
  bool _sending = false;
  bool _fetching = false;
  String? _error;
  Timer? _timer;

  int get _lastId => _messages.isEmpty ? 0 : _messages.last.id;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _fetch();
    _timer = Timer.periodic(_pollEvery, (_) => _fetch());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    _text.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _fetch();
  }

  Future<void> _fetch() async {
    if (_fetching) return;
    _fetching = true;
    try {
      final chat = await widget.orders.chat(widget.orderId, after: _lastId);
      if (!mounted) return;
      setState(() {
        _otherName = chat.otherName;
        _bookTitle = chat.bookTitle;
        _canSend = chat.canSend;
        _loaded = true;
        _error = null;
        final known = _messages.map((m) => m.id).toSet();
        _messages.addAll(chat.messages.where((m) => !known.contains(m.id)));
      });
    } on ApiException catch (e) {
      if (mounted && !_loaded) setState(() => _error = e.message);
    } catch (_) {
      // Offline for a moment: the next tick tries again.
    } finally {
      _fetching = false;
    }
  }

  Future<void> _send([String? preset]) async {
    final body = (preset ?? _text.text).trim();
    if (body.isEmpty || _sending) return;
    setState(() => _sending = true);
    try {
      final sent = await widget.orders.sendMessage(widget.orderId, body);
      if (!mounted) return;
      setState(() {
        if (!_messages.any((m) => m.id == sent.id)) _messages.add(sent);
        if (preset == null) _text.clear();
      });
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
        if (e.code == 'CHAT_CLOSED') setState(() => _canSend = false);
      }
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  List<String> get _quickReplies => widget.pickup
      ? (widget.iAmBuyer
          ? ['Hi! Where can we meet?', 'Which day and time suits you?', "I'm here 👋", 'Running 10 minutes late']
          : ['Hi! Can we meet at a public place near you?', 'Which day and time suits you?', "I'm here 👋", 'Running 10 minutes late'])
      : (widget.iAmBuyer
          ? ['Hi! When will you ship it?', 'Got the book, thank you!']
          : ['Hi! I will ship it soon.', 'Shipped! Check the tracking note.']);

  @override
  Widget build(BuildContext context) {
    final showPhoneHint = _phoneLike.hasMatch(_text.text);
    return Scaffold(
      appBar: AppBar(
        actions: [
          if (widget.otherUserId != null)
            SafetyMenu(
              safety: SafetyApi(widget.orders.api),
              userId: widget.otherUserId!,
              userName: _otherName ?? 'this person',
              onBlocked: () => setState(() => _canSend = false),
            ),
        ],
        title: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(_otherName ?? 'Chat'),
          if (_bookTitle != null)
            Text(_bookTitle!, style: TextStyle(fontSize: 13, color: context.pal.muted), overflow: TextOverflow.ellipsis),
        ]),
      ),
      body: SafeArea(
        child: Column(children: [
          Container(
            width: double.infinity,
            color: context.pal.primaryTint,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            child: Text(
              widget.pickup
                  ? '🔒 No need to share your phone number. Meet in a busy public place, and check the book before the code is shared.'
                  : '🔒 No need to share your phone number. Keep all order talk here.',
              style: const TextStyle(fontSize: 13),
            ),
          ),
          Expanded(child: _list(context)),
          if (_canSend && _loaded && _messages.isEmpty) _quickReplyChips(),
          if (showPhoneHint)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
              child: Text(
                'Looks like a phone number. You can keep chatting here instead; sharing it is up to you.',
                style: TextStyle(fontSize: 12, color: context.pal.muted),
              ),
            ),
          _canSend ? _composer(context) : _closedNote(context),
        ]),
      ),
    );
  }

  Widget _list(BuildContext context) {
    if (_error != null) return Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(_error!)));
    if (!_loaded) return const Center(child: CircularProgressIndicator());
    if (_messages.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Text(
            widget.pickup ? 'Say hi and agree on a place, day and time to meet.' : 'Say hi! Ask anything about the order.',
            textAlign: TextAlign.center,
            style: TextStyle(color: context.pal.muted),
          ),
        ),
      );
    }
    final items = _messages.reversed.toList();
    return ListView.builder(
      reverse: true,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      itemCount: items.length,
      itemBuilder: (context, i) => _Bubble(message: items[i]),
    );
  }

  Widget _quickReplyChips() => SizedBox(
        height: 48,
        child: ListView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          children: [
            for (final q in _quickReplies)
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ActionChip(label: Text(q), onPressed: _sending ? null : () => _send(q)),
              ),
          ],
        ),
      );

  Widget _composer(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(12, 8, 8, 12),
        child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Expanded(
            child: TextField(
              controller: _text,
              minLines: 1,
              maxLines: 5,
              maxLength: 1000,
              textCapitalization: TextCapitalization.sentences,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(hintText: 'Message', counterText: ''),
            ),
          ),
          const SizedBox(width: 6),
          IconButton.filled(
            tooltip: 'Send',
            onPressed: _sending || _text.text.trim().isEmpty ? null : () => _send(),
            icon: _sending
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.send),
          ),
        ]),
      );

  Widget _closedNote(BuildContext context) => Padding(
        padding: const EdgeInsets.all(16),
        child: Text('This order is closed, so the chat is read-only.',
            textAlign: TextAlign.center, style: TextStyle(color: context.pal.muted)),
      );
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.message});

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final mine = message.mine;
    final t = message.createdAt;
    final time = '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';
    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.78),
        child: Container(
          margin: const EdgeInsets.symmetric(vertical: 3),
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 6),
          decoration: BoxDecoration(
            color: mine ? context.pal.primary : context.pal.primaryTint,
            borderRadius: BorderRadius.only(
              topLeft: const Radius.circular(16),
              topRight: const Radius.circular(16),
              bottomLeft: Radius.circular(mine ? 16 : 4),
              bottomRight: Radius.circular(mine ? 4 : 16),
            ),
          ),
          child: Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            SelectableText(
              message.body,
              style: TextStyle(color: mine ? context.pal.onPrimary : null, fontSize: 15),
            ),
            const SizedBox(height: 2),
            Text(time,
                style: TextStyle(fontSize: 11, color: mine ? context.pal.onPrimary.withValues(alpha: 0.8) : context.pal.muted)),
          ]),
        ),
      ),
    );
  }
}
