import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/api_client.dart';
import '../books/book_models.dart';
import '../orders/order_models.dart';
import '../orders/orders_api.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';
import 'order_detail_screen.dart';

/// Choose pickup or delivery, use credits, add an address, and pay (test mode for now).
class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key, required this.orders, required this.book});

  final OrdersApi orders;
  final BookDetail book;

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _form = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _address = TextEditingController();
  final _city = TextEditingController();
  final _pincode = TextEditingController();

  late bool _pickup = widget.book.allowsPickup;
  bool _useCredits = false;
  Quote? _quote;
  String? _error;
  bool _paying = false;

  @override
  void initState() {
    super.initState();
    _refreshQuote();
  }

  @override
  void dispose() {
    for (final c in [_name, _phone, _address, _city, _pincode]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _refreshQuote() async {
    try {
      final q = await widget.orders.quote(widget.book.id, pickup: _pickup, credits: _useCredits ? 1 << 30 : 0);
      if (mounted) setState(() => _quote = q);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  void _choose(bool pickup) {
    setState(() => _pickup = pickup);
    _refreshQuote();
  }

  Future<void> _pay() async {
    setState(() => _error = null);
    if (!_pickup && !_form.currentState!.validate()) return;
    final quote = _quote;
    if (quote == null) return;
    setState(() => _paying = true);
    try {
      final order = await widget.orders.place(
        widget.book.id,
        pickup: _pickup,
        credits: quote.creditsUsed,
        address: _pickup
            ? null
            : ShippingAddress(
                name: _name.text.trim(),
                phone: _phone.text.trim(),
                address: _address.text.trim(),
                city: _city.text.trim(),
                pincode: _pincode.text.trim(),
              ),
      );
      if (!mounted) return;
      await Navigator.of(context).pushReplacement(MaterialPageRoute<void>(
        builder: (_) => OrderDetailScreen(orders: widget.orders, orderId: order.id, justPlaced: true),
      ));
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _paying = false);
    }
  }

  String? _required(String? v) => v == null || v.trim().isEmpty ? 'Required' : null;

  @override
  Widget build(BuildContext context) {
    final book = widget.book;
    final q = _quote;
    return Scaffold(
      appBar: AppBar(title: const Text('Checkout')),
      body: Form(
        key: _form,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(book.title, style: Theme.of(context).textTheme.titleLarge),
            Text('from ${book.sellerName ?? 'a reader'} · ${book.condition.label} · ${book.city}',
                style: TextStyle(color: context.pal.muted)),
            const SizedBox(height: 20),
            _label('How do you want it?'),
            if (book.allowsPickup)
              _MethodTile(
                selected: _pickup,
                title: 'Local pickup',
                subtitle: 'Meet the seller in ${book.city} · show your 4-digit code',
                price: 'No delivery fee',
                onTap: () => _choose(true),
              ),
            if (book.allowsShipping)
              _MethodTile(
                selected: !_pickup,
                title: 'Home delivery',
                subtitle: 'The seller sends it by courier',
                price: '+₹60',
                onTap: () => _choose(false),
              ),
            if (!_pickup) ...[
              const SizedBox(height: 12),
              _label('Delivery address'),
              TextFormField(controller: _name, decoration: const InputDecoration(labelText: 'Full name'), validator: _required),
              const SizedBox(height: 10),
              TextFormField(
                controller: _phone,
                keyboardType: TextInputType.phone,
                inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9+\s]'))],
                decoration: const InputDecoration(labelText: 'Mobile number', prefixText: '+91  '),
                validator: _required,
              ),
              const SizedBox(height: 10),
              TextFormField(
                controller: _address,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'House, street, area'),
                validator: _required,
              ),
              const SizedBox(height: 10),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(
                  flex: 3,
                  child: TextFormField(controller: _city, decoration: const InputDecoration(labelText: 'City'), validator: _required),
                ),
                const SizedBox(width: 10),
                Expanded(
                  flex: 2,
                  child: TextFormField(
                    controller: _pincode,
                    keyboardType: TextInputType.number,
                    maxLength: 6,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    decoration: const InputDecoration(labelText: 'Pincode', counterText: ''),
                    validator: (v) => RegExp(r'^\d{6}$').hasMatch(v ?? '') ? null : '6 digits',
                  ),
                ),
              ]),
            ],
            const SizedBox(height: 16),
            if (q != null && q.creditBalance > 0)
              Container(
                decoration: BoxDecoration(color: context.pal.primaryTint, borderRadius: BorderRadius.circular(14)),
                child: SwitchListTile(
                  value: _useCredits,
                  onChanged: (v) {
                    setState(() => _useCredits = v);
                    _refreshQuote();
                  },
                  title: Text('Use ${q.maxCredits} credits'),
                  subtitle: Text('You have ${q.creditBalance} · credits pay for the book only'),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
              ),
            const SizedBox(height: 16),
            if (q == null)
              const Center(child: Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator()))
            else
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: context.pal.surface,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: context.pal.border),
                ),
                child: Column(children: [
                  _row('Book price', '₹${q.bookPrice}'),
                  if (q.creditsUsed > 0) _row('Credits used', '−₹${q.creditsUsed}', highlight: true),
                  if (q.shippingFee > 0) _row('Delivery', '₹${q.shippingFee}'),
                  _row('Platform fee', '₹${q.platformFee}'),
                  const Divider(height: 20),
                  _row('You pay', '₹${q.cashToPay}', bold: true),
                ]),
              ),
            const SizedBox(height: 12),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Icon(Icons.verified_user_outlined, size: 18, color: context.pal.primary),
              const SizedBox(width: 8),
              Expanded(
                child: Text('Your money is held safely. The seller is paid only after you get the book.',
                    style: TextStyle(fontSize: 12, color: context.pal.muted)),
              ),
            ]),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: context.pal.accentTint,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: context.pal.accentBorder),
              ),
              child: const Text('Test mode: no real money is charged yet.', style: TextStyle(fontSize: 13)),
            ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text(_error!, style: TextStyle(color: context.pal.danger)),
              ),
            const SizedBox(height: 16),
            BusyButton(
              label: q == null ? 'Pay' : 'Pay ₹${q.cashToPay}',
              busy: _paying,
              onPressed: q == null ? null : _pay,
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Text(text, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: context.pal.muted)),
      );

  Widget _row(String label, String value, {bool bold = false, bool highlight = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(children: [
          Expanded(child: Text(label, style: TextStyle(color: bold ? null : context.pal.muted, fontWeight: bold ? FontWeight.w700 : null))),
          Text(value,
              style: TextStyle(
                fontWeight: bold || highlight ? FontWeight.w700 : null,
                fontSize: bold ? 17 : null,
                color: highlight ? context.pal.primary : null,
              )),
        ]),
      );
}

class _MethodTile extends StatelessWidget {
  const _MethodTile({
    required this.selected,
    required this.title,
    required this.subtitle,
    required this.price,
    required this.onTap,
  });

  final bool selected;
  final String title;
  final String subtitle;
  final String price;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Semantics(
        selected: selected,
        button: true,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: context.pal.surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: selected ? context.pal.primary : context.pal.border, width: selected ? 2 : 1),
            ),
            child: Row(children: [
              Icon(selected ? Icons.radio_button_checked : Icons.radio_button_off,
                  color: selected ? context.pal.primary : context.pal.muted),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
                  Text(subtitle, style: TextStyle(fontSize: 12, color: context.pal.muted)),
                ]),
              ),
              Text(price, style: const TextStyle(fontWeight: FontWeight.w600)),
            ]),
          ),
        ),
      ),
    );
  }
}
