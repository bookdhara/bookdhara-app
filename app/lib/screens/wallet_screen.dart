import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../orders/order_models.dart';
import '../orders/orders_api.dart';
import '../theme.dart';

class WalletScreen extends StatefulWidget {
  const WalletScreen({super.key, required this.orders});

  final OrdersApi orders;

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  Wallet? _wallet;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final w = await widget.orders.wallet();
      if (mounted) setState(() => _wallet = w);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final w = _wallet;
    return Scaffold(
      appBar: AppBar(title: const Text('Wallet')),
      body: _error != null
          ? Center(child: Text(_error!))
          : w == null
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(color: context.pal.primary, borderRadius: BorderRadius.circular(20)),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text('Credits', style: TextStyle(color: context.pal.onPrimary)),
                          Text('${w.credits}',
                              style: TextStyle(color: context.pal.onPrimary, fontSize: 40, fontWeight: FontWeight.w800)),
                          Text('1 credit = ₹1 on any book', style: TextStyle(color: context.pal.onPrimary, fontSize: 12)),
                          const SizedBox(height: 14),
                          Row(children: [
                            _Stat(label: 'On hold', value: '${w.creditsOnHold} credits'),
                            const SizedBox(width: 10),
                            _Stat(label: 'Cash earned', value: '₹${w.cashEarned}'),
                          ]),
                        ]),
                      ),
                      const SizedBox(height: 8),
                      Text('"On hold" credits arrive when the buyer gets the book. Cash payouts to your bank start when payments go live.',
                          style: TextStyle(fontSize: 12, color: context.pal.muted)),
                      const SizedBox(height: 20),
                      Text('History', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
                      const SizedBox(height: 8),
                      if (w.history.isEmpty)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          child: Text('Sell a book to earn your first credits.', style: TextStyle(color: context.pal.muted)),
                        ),
                      for (final e in w.history)
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: CircleAvatar(
                            backgroundColor: e.amount > 0 ? context.pal.primaryTint : context.pal.accentTint,
                            child: Icon(e.amount > 0 ? Icons.add : Icons.remove,
                                color: e.amount > 0 ? context.pal.primary : context.pal.ink),
                          ),
                          title: Text(e.note),
                          subtitle: Text(shortDate(e.createdAt)),
                          trailing: Text('${e.amount > 0 ? '+' : ''}${e.amount}',
                              style: TextStyle(
                                  fontWeight: FontWeight.w700, color: e.amount > 0 ? context.pal.primary : null)),
                        ),
                    ],
                  ),
                ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: context.pal.onPrimary.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: TextStyle(color: context.pal.onPrimary, fontSize: 11)),
          Text(value, style: TextStyle(color: context.pal.onPrimary, fontWeight: FontWeight.w700)),
        ]),
      ),
    );
  }
}
