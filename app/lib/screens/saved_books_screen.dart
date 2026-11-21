import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../books/book_models.dart';
import '../books/books_api.dart';
import '../theme.dart';
import '../widgets/book_cover.dart';
import 'book_detail_screen.dart';

/// Books I tapped ♡ on.
class SavedBooksScreen extends StatefulWidget {
  const SavedBooksScreen({super.key, required this.books});

  final BooksApi books;

  @override
  State<SavedBooksScreen> createState() => _SavedBooksScreenState();
}

class _SavedBooksScreenState extends State<SavedBooksScreen> {
  List<BookSummary>? _items;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = await widget.books.saved();
      if (mounted) setState(() => _items = items);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _open(BookSummary b) async {
    await Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => BookDetailScreen(books: widget.books, bookId: b.id),
    ));
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final items = _items;
    return Scaffold(
      appBar: AppBar(title: const Text('Saved books')),
      body: _error != null
          ? Center(child: Text(_error!))
          : items == null
              ? const Center(child: CircularProgressIndicator())
              : items.isEmpty
                  ? Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32),
                        child: Text('Tap ♡ on a book to save it here.',
                            textAlign: TextAlign.center, style: TextStyle(color: context.pal.muted)),
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.separated(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        itemCount: items.length,
                        separatorBuilder: (context, index) => const Divider(height: 1),
                        itemBuilder: (context, i) {
                          final b = items[i];
                          return ListTile(
                            onTap: () => _open(b),
                            leading: SizedBox(
                              width: 44,
                              child: BookCover(
                                title: b.title,
                                height: 60,
                                radius: 6,
                                imageUrl: b.coverPhotoUrl == null ? null : widget.books.api.url(b.coverPhotoUrl!),
                              ),
                            ),
                            title: Text(b.title, maxLines: 1, overflow: TextOverflow.ellipsis),
                            subtitle: Text(b.isSold ? 'Sold' : '₹${b.price} · ${b.city}',
                                style: TextStyle(color: b.isSold ? context.pal.danger : context.pal.muted)),
                            trailing: const Icon(Icons.chevron_right),
                          );
                        },
                      ),
                    ),
    );
  }
}
