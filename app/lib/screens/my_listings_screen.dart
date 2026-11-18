import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../books/book_models.dart';
import '../books/books_api.dart';
import '../theme.dart';
import '../widgets/book_cover.dart';
import 'book_detail_screen.dart';

class MyListingsScreen extends StatefulWidget {
  const MyListingsScreen({super.key, required this.books});

  final BooksApi books;

  @override
  State<MyListingsScreen> createState() => _MyListingsScreenState();
}

class _MyListingsScreenState extends State<MyListingsScreen> {
  List<BookSummary>? _items;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = await widget.books.myListings();
      if (mounted) setState(() => _items = items);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = _items;
    return Scaffold(
      appBar: AppBar(title: const Text('My listings')),
      body: _error != null
          ? Center(child: Text(_error!))
          : items == null
              ? const Center(child: CircularProgressIndicator())
              : items.isEmpty
                  ? const Center(
                      child: Padding(
                        padding: EdgeInsets.all(24),
                        child: Text("You haven't listed any books yet.",
                            textAlign: TextAlign.center, style: TextStyle(color: AppColors.muted)),
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.separated(
                        padding: const EdgeInsets.all(16),
                        itemCount: items.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 12),
                        itemBuilder: (context, i) {
                          final book = items[i];
                          return InkWell(
                            borderRadius: BorderRadius.circular(12),
                            onTap: () async {
                              await Navigator.of(context).push(MaterialPageRoute<bool>(
                                builder: (_) => BookDetailScreen(books: widget.books, bookId: book.id),
                              ));
                              _load();
                            },
                            child: Row(children: [
                              SizedBox(
                                width: 56,
                                child: BookCover(
                                  title: book.title,
                                  height: 76,
                                  radius: 8,
                                  imageUrl: book.coverPhotoUrl == null
                                      ? null
                                      : widget.books.photoUrl(book.coverPhotoUrl!),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                  Text(book.title, style: const TextStyle(fontWeight: FontWeight.w600)),
                                  const SizedBox(height: 2),
                                  Text('₹${book.price} · ${book.condition.label}',
                                      style: const TextStyle(color: AppColors.muted, fontSize: 13)),
                                ]),
                              ),
                              const Icon(Icons.chevron_right, color: AppColors.muted),
                            ]),
                          );
                        },
                      ),
                    ),
    );
  }
}
