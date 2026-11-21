import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/api_client.dart';
import '../orders/order_models.dart';
import '../orders/orders_api.dart';
import '../theme.dart';
import '../widgets/book_cover.dart';

class OrderDetailScreen extends StatefulWidget {
  const OrderDetailScreen({super.key, required this.orders, required this.orderId, this.justPlaced = false});

  final OrdersApi orders;
  final int orderId;
  final bool justPlaced;

  @override
  State<OrderDetailScreen> createState() => _OrderDetailScreenState();
}

class _OrderDetailScreenState extends State<OrderDetailScreen> {
  OrderView? _order;
  String? _error;
  bool _busy = false;
  final _code = TextEditingController();
  final _tracking = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _code.dispose();
    _tracking.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final o = await widget.orders.get(widget.orderId);
      if (mounted) setState(() => _order = o);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _run(Future<OrderView> Function() action, String done) async {
    setState(() => _busy = true);
    try {
      final o = await action();
      if (!mounted) return;
      setState(() => _order = o);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(done)));
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _cancel() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cancel this order?'),
        content: const Text('Any credits used go back to the buyer, and the book is listed again.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Keep order')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Cancel order')),
        ],
      ),
    );
    if (ok == true) await _run(() => widget.orders.cancel(widget.orderId, null), 'Order cancelled');
  }

  @override
  Widget build(BuildContext context) {
    final o = _order;
    return Scaffold(
      appBar: AppBar(title: Text(o == null ? 'Order' : 'Order #${o.id}')),
      body: _error != null
          ? Center(child: Text(_error!))
          : o == null
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(onRefresh: _load, child: _body(context, o)),
    );
  }

  Widget _body(BuildContext context, OrderView o) {
    final textTheme = Theme.of(context).textTheme;
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        if (widget.justPlaced && o.status == OrderStatus.placed)
          _Banner(
            color: context.pal.primaryTint,
            text: o.isPickup
                ? 'Order placed! Meet the seller and show them your code when you have the book.'
                : 'Order placed! The seller will send your book soon.',
          ),
        Row(children: [
          SizedBox(
            width: 64,
            child: BookCover(
              title: o.bookTitle,
              height: 88,
              radius: 8,
              imageUrl: o.coverPhotoUrl == null ? null : widget.orders.api.url(o.coverPhotoUrl!),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(o.bookTitle, style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
              Text(o.isBuyer ? 'from ${o.otherPartyName}' : 'bought by ${o.otherPartyName}',
                  style: TextStyle(color: context.pal.muted)),
              const SizedBox(height: 6),
              _StatusChip(status: o.status),
            ]),
          ),
        ]),
        const SizedBox(height: 20),
        _Steps(order: o),
        const SizedBox(height: 20),

        // Pickup: the buyer shows the code; the seller types it in.
        if (o.isBuyer && o.handoverCode != null) ...[
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(color: context.pal.primary, borderRadius: BorderRadius.circular(20)),
            child: Column(children: [
              Text('Your handover code', style: TextStyle(color: context.pal.onPrimary)),
              const SizedBox(height: 8),
              Text(o.handoverCode!,
                  style: TextStyle(
                      color: context.pal.onPrimary, fontSize: 40, fontWeight: FontWeight.w800, letterSpacing: 12)),
              const SizedBox(height: 8),
              Text('Tell the seller this code only after you have checked the book.',
                  textAlign: TextAlign.center, style: TextStyle(color: context.pal.onPrimary, fontSize: 13)),
            ]),
          ),
          const SizedBox(height: 16),
        ],
        if (o.canEnterHandoverCode) ...[
          Text('Handed over the book?', style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 4),
          Text("Ask the buyer for the 4-digit code in their app. You're paid once it matches.",
              style: TextStyle(color: context.pal.muted)),
          const SizedBox(height: 10),
          Row(children: [
            Expanded(
              child: TextField(
                controller: _code,
                keyboardType: TextInputType.number,
                maxLength: 4,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                textAlign: TextAlign.center,
                // Rebuild so Confirm turns on as soon as 4 digits are typed.
                onChanged: (_) => setState(() {}),
                onSubmitted: (_) {
                  if (!_busy && _code.text.length == 4) {
                    _run(() => widget.orders.handover(o.id, _code.text), 'Done! The sale is complete.');
                  }
                },
                style: const TextStyle(fontSize: 22, letterSpacing: 10, fontWeight: FontWeight.w700),
                decoration: const InputDecoration(counterText: '', hintText: '----'),
              ),
            ),
            const SizedBox(width: 10),
            SizedBox(
              height: 56,
              child: FilledButton(
                style: FilledButton.styleFrom(minimumSize: const Size(110, 56)),
                onPressed: _busy || _code.text.length != 4
                    ? null
                    : () => _run(() => widget.orders.handover(o.id, _code.text), 'Done! The sale is complete.'),
                child: const Text('Confirm'),
              ),
            ),
          ]),
          const SizedBox(height: 16),
        ],

        // Delivery.
        if (o.address != null) ...[
          _Card(
            title: o.isBuyer ? 'Delivering to' : 'Send it to',
            child: Text('${o.address!.name}\n${o.address!.address}\n${o.address!.city} ${o.address!.pincode}\n${o.address!.phone}',
                style: const TextStyle(height: 1.45)),
          ),
          const SizedBox(height: 12),
        ],
        if (o.trackingNote != null) ...[
          _Card(title: 'Tracking', child: Text(o.trackingNote!)),
          const SizedBox(height: 12),
        ],
        if (o.canShip) ...[
          TextField(
            controller: _tracking,
            decoration: const InputDecoration(labelText: 'Courier and tracking number (optional)'),
          ),
          const SizedBox(height: 10),
          FilledButton(
            onPressed: _busy
                ? null
                : () => _run(() => widget.orders.ship(o.id, _tracking.text.trim().isEmpty ? null : _tracking.text.trim()),
                    'Marked as shipped'),
            child: const Text("I've shipped it"),
          ),
          const SizedBox(height: 12),
        ],
        if (o.canConfirmReceived) ...[
          FilledButton(
            onPressed: _busy ? null : () => _run(() => widget.orders.confirmReceived(o.id), 'Enjoy your book!'),
            child: const Text('I received the book'),
          ),
          const SizedBox(height: 4),
          Text('The seller is paid when you confirm.',
              textAlign: TextAlign.center, style: TextStyle(fontSize: 12, color: context.pal.muted)),
          const SizedBox(height: 12),
        ],

        // Money.
        _Card(
          title: 'Payment',
          child: Column(children: [
            _row(context, 'Book price', '₹${o.bookPrice}'),
            if (o.creditsUsed > 0) _row(context, 'Credits used', '−₹${o.creditsUsed}'),
            if (o.shippingFee > 0) _row(context, 'Delivery', '₹${o.shippingFee}'),
            _row(context, 'Platform fee', '₹${o.platformFee}'),
            const Divider(height: 16),
            _row(context, o.isBuyer ? 'You paid' : 'Buyer paid', '₹${o.cashPaid}${o.testPayment ? ' (test)' : ''}', bold: true),
            if (!o.isBuyer && o.sellerPayoutAmount != null)
              _row(context, 'You get',
                  o.sellerPayoutCredits ? '${o.sellerPayoutAmount} credits' : '₹${o.sellerPayoutAmount}',
                  bold: true),
          ]),
        ),
        if (o.cancelReason != null) ...[
          const SizedBox(height: 12),
          _Banner(color: context.pal.accentTint, text: o.cancelReason!),
        ],
        if (o.canCancel) ...[
          const SizedBox(height: 20),
          TextButton(
            onPressed: _busy ? null : _cancel,
            style: TextButton.styleFrom(foregroundColor: context.pal.danger),
            child: const Text('Cancel order'),
          ),
        ],
      ],
    );
  }

  Widget _row(BuildContext context, String label, String value, {bool bold = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 3),
        child: Row(children: [
          Expanded(child: Text(label, style: TextStyle(color: bold ? null : context.pal.muted))),
          Text(value, style: TextStyle(fontWeight: bold ? FontWeight.w700 : null)),
        ]),
      );
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final OrderStatus status;

  @override
  Widget build(BuildContext context) {
    final done = status == OrderStatus.completed;
    final cancelled = status == OrderStatus.cancelled;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: cancelled ? context.pal.accentTint : context.pal.primaryTint,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Text(status.label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w700,
            color: done || !cancelled ? context.pal.primary : context.pal.ink,
          )),
    );
  }
}

/// The order's steps, with the ones already done ticked.
class _Steps extends StatelessWidget {
  const _Steps({required this.order});

  final OrderView order;

  @override
  Widget build(BuildContext context) {
    if (order.status == OrderStatus.cancelled) return const SizedBox.shrink();
    final steps = order.isPickup
        ? ['Order placed', 'Meet and hand over', 'Completed']
        : ['Order placed', 'Shipped', 'Delivered'];
    final reached = switch (order.status) {
      OrderStatus.placed => 0,
      OrderStatus.shipped => 1,
      OrderStatus.completed => 2,
      OrderStatus.cancelled => -1,
    };
    return Column(children: [
      for (var i = 0; i < steps.length; i++)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(children: [
            Icon(i <= reached ? Icons.check_circle : Icons.radio_button_unchecked,
                color: i <= reached ? context.pal.primary : context.pal.muted, size: 22),
            const SizedBox(width: 10),
            Text(steps[i], style: TextStyle(fontWeight: i == reached + 1 ? FontWeight.w700 : null)),
          ]),
        ),
    ]);
  }
}

class _Card extends StatelessWidget {
  const _Card({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: context.pal.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: context.pal.border),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: context.pal.muted)),
        const SizedBox(height: 8),
        child,
      ]),
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.color, required this.text});

  final Color color;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(12)),
      child: Text(text, style: const TextStyle(fontWeight: FontWeight.w600)),
    );
  }
}
