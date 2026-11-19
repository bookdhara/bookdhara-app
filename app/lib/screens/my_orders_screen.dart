import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../orders/order_models.dart';
import '../orders/orders_api.dart';
import '../theme.dart';
import '../widgets/book_cover.dart';
import 'order_detail_screen.dart';

/// Order history: books I bought, and books I sold.
class MyOrdersScreen extends StatelessWidget {
  const MyOrdersScreen({super.key, required this.orders});

  final OrdersApi orders;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('My orders'),
          bottom: const TabBar(tabs: [Tab(text: 'Bought'), Tab(text: 'Sold')]),
        ),
        body: TabBarView(children: [
          _OrderList(orders: orders, selling: false),
          _OrderList(orders: orders, selling: true),
        ]),
      ),
    );
  }
}

class _OrderList extends StatefulWidget {
  const _OrderList({required this.orders, required this.selling});

  final OrdersApi orders;
  final bool selling;

  @override
  State<_OrderList> createState() => _OrderListState();
}

class _OrderListState extends State<_OrderList> {
  List<OrderView>? _items;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = await widget.orders.mine(selling: widget.selling);
      if (mounted) setState(() => _items = items);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = _items;
    if (_error != null) return Center(child: Text(_error!));
    if (items == null) return const Center(child: CircularProgressIndicator());
    if (items.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(widget.selling ? "You haven't sold any books yet." : "You haven't bought any books yet.",
              style: TextStyle(color: context.pal.muted)),
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: items.length,
        separatorBuilder: (_, __) => const SizedBox(height: 12),
        itemBuilder: (context, i) {
          final o = items[i];
          final needsYou = o.canShip || o.canEnterHandoverCode || o.canConfirmReceived;
          return InkWell(
            borderRadius: BorderRadius.circular(12),
            onTap: () async {
              await Navigator.of(context).push(MaterialPageRoute<void>(
                builder: (_) => OrderDetailScreen(orders: widget.orders, orderId: o.id),
              ));
              _load();
            },
            child: Row(children: [
              SizedBox(
                width: 52,
                child: BookCover(
                  title: o.bookTitle,
                  height: 72,
                  radius: 8,
                  imageUrl: o.coverPhotoUrl == null ? null : widget.orders.api.url(o.coverPhotoUrl!),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(o.bookTitle, style: const TextStyle(fontWeight: FontWeight.w600)),
                  Text('${o.isPickup ? 'Pickup' : 'Delivery'} · ${shortDate(o.createdAt)} · ${o.status.label}',
                      style: TextStyle(fontSize: 13, color: context.pal.muted)),
                  if (needsYou)
                    Text('Needs your action',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: context.pal.primary)),
                ]),
              ),
              Text('₹${o.cashPaid}', style: const TextStyle(fontWeight: FontWeight.w700)),
            ]),
          );
        },
      ),
    );
  }
}
