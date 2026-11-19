import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../books/book_models.dart';
import '../books/books_api.dart';
import '../theme.dart';
import '../widgets/book_cover.dart';

class BookDetailScreen extends StatefulWidget {
  const BookDetailScreen({super.key, required this.books, required this.bookId});

  final BooksApi books;
  final int bookId;

  @override
  State<BookDetailScreen> createState() => _BookDetailScreenState();
}

class _BookDetailScreenState extends State<BookDetailScreen> {
  BookDetail? _book;
  String? _error;
  int _photo = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final book = await widget.books.detail(widget.bookId);
      if (mounted) setState(() => _book = book);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _remove() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Remove this listing?'),
        content: Text('Buyers will no longer see it.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: Text('Remove')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await widget.books.remove(widget.bookId);
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final book = _book;
    return Scaffold(
      appBar: AppBar(title: Text(book?.title ?? '')),
      body: _error != null
          ? Center(child: Padding(padding: EdgeInsets.all(24), child: Text(_error!)))
          : book == null
              ? Center(child: CircularProgressIndicator())
              : _details(context, book),
      bottomNavigationBar: book == null ? null : _actions(context, book),
    );
  }

  Widget _details(BuildContext context, BookDetail book) {
    final textTheme = Theme.of(context).textTheme;
    final off = discountPercent(book.price, book.mrp);
    final urls = book.photoUrls.map(widget.books.photoUrl).toList();

    return ListView(
      padding: EdgeInsets.all(20),
      children: [
        Center(
          child: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: 360),
            child: BookCover(title: book.title, imageUrl: urls.isEmpty ? null : urls[_photo], height: 300),
          ),
        ),
        if (urls.length > 1) ...[
          SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var i = 0; i < urls.length; i++)
                Padding(
                  padding: EdgeInsets.symmetric(horizontal: 4),
                  child: InkWell(
                    onTap: () => setState(() => _photo = i),
                    borderRadius: BorderRadius.circular(8),
                    child: Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: i == _photo ? context.pal.primary : Colors.transparent, width: 2),
                      ),
                      child: SizedBox(
                        width: 52,
                        child: BookCover(title: book.title, imageUrl: urls[i], height: 64, radius: 6),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ],
        SizedBox(height: 20),
        Text(book.title, style: textTheme.headlineMedium?.copyWith(fontSize: 24)),
        if (book.author != null) ...[
          SizedBox(height: 4),
          Text(book.author!, style: TextStyle(color: context.pal.muted, fontSize: 15)),
        ],
        SizedBox(height: 12),
        Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Text('₹${book.price}', style: TextStyle(fontSize: 26, fontWeight: FontWeight.w700)),
          if (book.mrp != null) ...[
            SizedBox(width: 10),
            Text('MRP ₹${book.mrp}',
                style: TextStyle(color: context.pal.muted, decoration: TextDecoration.lineThrough)),
          ],
          if (off != null) ...[
            SizedBox(width: 10),
            Container(
              padding: EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(color: context.pal.primaryTint, borderRadius: BorderRadius.circular(8)),
              child: Text('$off% off',
                  style: TextStyle(color: context.pal.primary, fontWeight: FontWeight.w600, fontSize: 13)),
            ),
          ],
        ]),
        SizedBox(height: 16),
        Row(children: [
          _Fact(label: 'Condition', value: book.condition.label),
          SizedBox(width: 8),
          _Fact(label: 'City', value: book.city),
          SizedBox(width: 8),
          _Fact(
            label: 'Get it by',
            value: [if (book.allowsPickup) 'Pickup', if (book.allowsShipping) 'Delivery'].join(' / '),
          ),
        ]),
        if (book.note != null) ...[
          SizedBox(height: 16),
          _Panel(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text("Seller's note", style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
              SizedBox(height: 6),
              Text(book.note!, style: TextStyle(height: 1.45)),
            ]),
          ),
        ],
        SizedBox(height: 16),
        if (book.mine)
          _Panel(
            color: context.pal.primaryTint,
            child: Text(
              book.payout == 'CREDITS'
                  ? 'When it sells, you get ${book.payoutAmount} credits.'
                  : 'When it sells, you get ₹${book.payoutAmount}.',
              style: TextStyle(fontWeight: FontWeight.w600),
            ),
          )
        else
          Row(children: [
            CircleAvatar(
              backgroundColor: context.pal.primaryTint,
              foregroundColor: context.pal.primary,
              child: Text((book.sellerName ?? '?').characters.first.toUpperCase()),
            ),
            SizedBox(width: 12),
            Text('Sold by ${book.sellerName ?? 'a reader'}', style: TextStyle(fontWeight: FontWeight.w600)),
          ]),
        if (book.isbn != null) ...[
          SizedBox(height: 16),
          Text('ISBN ${book.isbn}', style: TextStyle(fontSize: 12, color: context.pal.muted)),
        ],
      ],
    );
  }

  Widget _actions(BuildContext context, BookDetail book) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(20, 8, 20, 12),
        child: book.mine
            ? OutlinedButton(
                onPressed: book.isAvailable ? _remove : null,
                style: OutlinedButton.styleFrom(
                  minimumSize: Size.fromHeight(52),
                  foregroundColor: context.pal.danger,
                ),
                child: Text('Remove listing'),
              )
            : FilledButton(
                onPressed: book.isAvailable
                    ? () => ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Buying opens in the next update')))
                    : null,
                child: Text(book.isAvailable ? 'Buy for ₹${book.price}' : 'No longer available'),
              ),
      ),
    );
  }
}

class _Fact extends StatelessWidget {
  _Fact({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: _Panel(
        padding: 10,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: TextStyle(fontSize: 11, color: context.pal.muted)),
          SizedBox(height: 2),
          Text(value, maxLines: 1, overflow: TextOverflow.ellipsis,
              style: TextStyle(fontWeight: FontWeight.w600)),
        ]),
      ),
    );
  }
}

class _Panel extends StatelessWidget {
  const _Panel({required this.child, this.color, this.padding = 14});

  final Widget child;
  final Color? color;
  final double padding;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(padding),
      decoration: BoxDecoration(
        color: color ?? context.pal.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: context.pal.border),
      ),
      child: child,
    );
  }
}
