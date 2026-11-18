import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../books/book_models.dart';
import '../books/books_api.dart';
import '../theme.dart';
import '../widgets/book_cover.dart';
import 'book_detail_screen.dart';
import 'my_listings_screen.dart';
import 'sell_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.auth, required this.books});

  final AuthStore auth;
  final BooksApi books;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _search = TextEditingController();
  List<BookSummary>? _items;
  String? _error;
  String _query = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final items = await widget.books.browse(query: _query);
      if (mounted) setState(() => _items = items);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  void _runSearch(String text) {
    _query = text;
    setState(() => _items = null);
    _load();
  }

  Future<void> _openSell() async {
    final created = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => SellScreen(books: widget.books)),
    );
    if (created == true && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Your book is listed! Find it under My listings.')),
      );
    }
  }

  void _openBook(BookSummary book) {
    Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => BookDetailScreen(books: widget.books, bookId: book.id),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final firstName = (widget.auth.user?.name ?? '').split(' ').first;

    return Scaffold(
      appBar: AppBar(
        titleSpacing: 20,
        title: Text('BookDhara', style: textTheme.titleLarge?.copyWith(color: AppColors.primary, fontSize: 24)),
        actions: [
          PopupMenuButton<String>(
            tooltip: 'Account',
            icon: const Icon(Icons.account_circle_outlined),
            onSelected: (value) {
              if (value == 'listings') {
                Navigator.of(context).push(MaterialPageRoute<void>(
                  builder: (_) => MyListingsScreen(books: widget.books),
                ));
              } else if (value == 'logout') {
                widget.auth.logout();
              }
            },
            itemBuilder: (_) => [
              PopupMenuItem<String>(enabled: false, child: Text(widget.auth.user?.phone ?? '')),
              const PopupMenuItem<String>(value: 'listings', child: Text('My listings')),
              const PopupMenuItem<String>(value: 'logout', child: Text('Log out')),
            ],
          ),
          const SizedBox(width: 8),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openSell,
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text('Sell a book'),
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: CustomScrollView(
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
              sliver: SliverList.list(children: [
                Text('Hi $firstName 👋', style: textTheme.titleLarge),
                const SizedBox(height: 14),
                TextField(
                  controller: _search,
                  textInputAction: TextInputAction.search,
                  onSubmitted: _runSearch,
                  decoration: InputDecoration(
                    hintText: 'Search title, author or ISBN',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: _query.isEmpty
                        ? null
                        : IconButton(
                            tooltip: 'Clear search',
                            icon: const Icon(Icons.close),
                            onPressed: () {
                              _search.clear();
                              _runSearch('');
                            },
                          ),
                  ),
                ),
                const SizedBox(height: 20),
                Text(_query.isEmpty ? 'Recently listed' : 'Results for "$_query"',
                    style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600)),
                const SizedBox(height: 12),
              ]),
            ),
            ..._content(textTheme),
            const SliverToBoxAdapter(child: SizedBox(height: 96)),
          ],
        ),
      ),
    );
  }

  List<Widget> _content(TextTheme textTheme) {
    if (_error != null) {
      return [
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(children: [
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: _load, child: const Text('Try again')),
            ]),
          ),
        ),
      ];
    }
    if (_items == null) {
      return [
        const SliverToBoxAdapter(
          child: Padding(
            padding: EdgeInsets.all(40),
            child: Center(child: CircularProgressIndicator()),
          ),
        ),
      ];
    }
    if (_items!.isEmpty) {
      return [
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(children: [
              const Icon(Icons.menu_book_outlined, size: 56, color: AppColors.muted),
              const SizedBox(height: 12),
              Text(_query.isEmpty ? 'No books listed yet' : 'No books match your search',
                  textAlign: TextAlign.center, style: textTheme.titleMedium),
              const SizedBox(height: 6),
              Text(
                _query.isEmpty ? 'Be the first to list one and start the chain.' : 'Try a different title or author.',
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.muted),
              ),
            ]),
          ),
        ),
      ];
    }
    return [
      SliverPadding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        sliver: SliverGrid.builder(
          gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
            maxCrossAxisExtent: 220,
            mainAxisSpacing: 16,
            crossAxisSpacing: 14,
            mainAxisExtent: 250,
          ),
          itemCount: _items!.length,
          itemBuilder: (_, i) => _BookCard(book: _items![i], books: widget.books, onTap: () => _openBook(_items![i])),
        ),
      ),
    ];
  }
}

class _BookCard extends StatelessWidget {
  const _BookCard({required this.book, required this.books, required this.onTap});

  final BookSummary book;
  final BooksApi books;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          BookCover(
            title: book.title,
            imageUrl: book.coverPhotoUrl == null ? null : books.photoUrl(book.coverPhotoUrl!),
          ),
          const SizedBox(height: 8),
          Text(book.title, maxLines: 1, overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontWeight: FontWeight.w600)),
          Text(
            [if (book.author != null) book.author!, book.condition.label].join(' · '),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 12, color: AppColors.muted),
          ),
          const SizedBox(height: 2),
          Row(children: [
            Text('₹${book.price}', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15)),
            if (book.mrp != null) ...[
              const SizedBox(width: 6),
              Text('₹${book.mrp}',
                  style: const TextStyle(
                      fontSize: 12, color: AppColors.muted, decoration: TextDecoration.lineThrough)),
            ],
            const Spacer(),
            Flexible(
              child: Text(book.city,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 11, color: AppColors.primary, fontWeight: FontWeight.w600)),
            ),
          ]),
        ],
      ),
    );
  }
}
