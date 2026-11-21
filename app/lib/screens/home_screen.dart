import 'dart:async';

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/api_client.dart';
import '../auth/auth_store.dart';
import '../books/book_models.dart';
import '../books/books_api.dart';
import '../deep_links.dart';
import '../orders/order_models.dart';
import '../orders/orders_api.dart';
import '../theme.dart';
import '../widgets/book_cover.dart';
import '../settings/app_settings.dart';
import 'book_detail_screen.dart';
import 'my_orders_screen.dart';
import 'order_detail_screen.dart';
import 'profile_screen.dart';
import 'sell_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.auth, required this.books, required this.settings});

  final AuthStore auth;
  final BooksApi books;
  final AppSettings settings;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  static const _alertedKey = 'alerted_orders';
  static const _checkEvery = Duration(seconds: 60);

  final _search = TextEditingController();
  late final OrdersApi _orders = OrdersApi(widget.books.api);
  List<OrderView> _attention = const [];
  Timer? _ordersTimer;
  bool _checking = false;
  List<BookSummary>? _items;
  String? _error;
  String _query = '';
  BrowseFilters _filters = const BrowseFilters();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
    _checkOrders();
    _ordersTimer = Timer.periodic(_checkEvery, (_) => _checkOrders());
    final linkedBook = DeepLinks.takeBookId();
    if (linkedBook != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        Navigator.of(context).push(MaterialPageRoute<void>(
          builder: (_) => BookDetailScreen(books: widget.books, bookId: linkedBook),
        ));
      });
    }
  }

  Future<void> _alertMe() async {
    try {
      await widget.books.addAlert(_query, city: _filters.city);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text("Done! We'll email you when \"$_query\" is listed${_filters.city == null ? '' : ' in ${_filters.city}'}."),
      ));
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _ordersTimer?.cancel();
    _search.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _checkOrders();
  }

  /// Looks for orders waiting on me. Something new pops up once; the banner stays until it's done.
  Future<void> _checkOrders() async {
    if (_checking) return;
    _checking = true;
    try {
      final list = await _orders.attention();
      if (!mounted) return;
      setState(() => _attention = list);

      final prefs = await SharedPreferences.getInstance();
      final alerted = prefs.getStringList(_alertedKey) ?? <String>[];
      final fresh = list.where((o) => !alerted.contains(_alertKey(o))).toList();
      // Only pop up when Home is on screen; otherwise wait until the person comes back.
      if (fresh.isEmpty || !mounted || ModalRoute.of(context)?.isCurrent != true) return;

      final updated = [...alerted, ...fresh.map(_alertKey)];
      await prefs.setStringList(_alertedKey, updated.length > 200 ? updated.sublist(updated.length - 200) : updated);
      if (mounted) await _showOrderPopup(fresh);
    } catch (_) {
      // Server asleep or offline: try again next time.
    } finally {
      _checking = false;
    }
  }

  static String _alertKey(OrderView o) =>
      '${o.id}:${o.status.api}${o.unreadMessages > 0 ? ':m${o.unreadMessages}' : ''}';

  Future<void> _showOrderPopup(List<OrderView> fresh) async {
    final first = fresh.first;
    final more = fresh.length - 1;
    final open = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        icon: Text(first.unreadMessages > 0 ? '💬' : (first.isBuyer ? '🚚' : '🎉'), style: const TextStyle(fontSize: 40)),
        title: Text(first.unreadMessages > 0
            ? 'New message'
            : (first.isBuyer ? 'Your book is on its way' : 'Your book sold!')),
        content: Text(first.nextStep + (more > 0 ? '\n\n+ $more more ${more == 1 ? 'order needs' : 'orders need'} you.' : '')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Later')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('View order')),
        ],
      ),
    );
    if (open == true && mounted) await _openOrder(first);
  }

  Future<void> _openOrder(OrderView o) async {
    await Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => OrderDetailScreen(orders: _orders, orderId: o.id),
    ));
    _checkOrders();
  }

  Future<void> _openAttention() async {
    if (_attention.length == 1) {
      await _openOrder(_attention.first);
      return;
    }
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => MyOrdersScreen(orders: _orders)));
    _checkOrders();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final items = await widget.books.browse(query: _query, filters: _filters);
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
        SnackBar(content: Text('Your book is listed! Find it under My listings.')),
      );
    }
  }

  Future<void> _openFilters() async {
    final chosen = await showModalBottomSheet<BrowseFilters>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _FiltersSheet(books: widget.books, initial: _filters),
    );
    if (chosen != null && mounted) {
      setState(() {
        _filters = chosen;
        _items = null;
      });
      _load();
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
        title: Text('BookDhara', style: textTheme.titleLarge?.copyWith(color: context.pal.primary, fontSize: 24)),
        actions: [
          IconButton(
            tooltip: 'Profile',
            onPressed: () async {
              await Navigator.of(context).push(MaterialPageRoute<void>(
                builder: (_) => ProfileScreen(auth: widget.auth, books: widget.books, settings: widget.settings),
              ));
              _checkOrders();
            },
            icon: Badge(
              isLabelVisible: _attention.isNotEmpty,
              label: Text('${_attention.length}'),
              child: CircleAvatar(
              radius: 16,
              backgroundColor: context.pal.primaryTint,
              foregroundColor: context.pal.primary,
              child: Text(
                firstName.isEmpty ? '?' : firstName.characters.first.toUpperCase(),
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
            ),
          ),
          SizedBox(width: 8),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openSell,
        backgroundColor: context.pal.primary,
        foregroundColor: context.pal.onPrimary,
        icon: Icon(Icons.add),
        label: Text('Sell a book'),
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: CustomScrollView(
          slivers: [
            SliverPadding(
              padding: EdgeInsets.fromLTRB(20, 16, 20, 8),
              sliver: SliverList.list(children: [
                Text('Hi $firstName 👋', style: textTheme.titleLarge),
                SizedBox(height: 14),
                if (_attention.isNotEmpty) ...[
                  _AttentionBanner(orders: _attention, onTap: _openAttention),
                  const SizedBox(height: 14),
                ],
                TextField(
                  controller: _search,
                  textInputAction: TextInputAction.search,
                  onSubmitted: _runSearch,
                  decoration: InputDecoration(
                    hintText: 'Search title, author or ISBN',
                    prefixIcon: Icon(Icons.search),
                    suffixIcon: _query.isEmpty
                        ? null
                        : IconButton(
                            tooltip: 'Clear search',
                            icon: Icon(Icons.close),
                            onPressed: () {
                              _search.clear();
                              _runSearch('');
                            },
                          ),
                  ),
                ),
                const SizedBox(height: 16),
                Row(children: [
                  Expanded(
                    child: Text(
                      _query.isEmpty
                          ? (_filters.activeCount == 0 ? 'Recently listed' : 'Filtered books')
                          : 'Results for "$_query"',
                      style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
                    ),
                  ),
                  Badge(
                    isLabelVisible: _filters.activeCount > 0,
                    label: Text('${_filters.activeCount}'),
                    child: OutlinedButton.icon(
                      onPressed: _openFilters,
                      icon: const Icon(Icons.tune, size: 18),
                      label: const Text('Filters'),
                    ),
                  ),
                ]),
                const SizedBox(height: 12),
              ]),
            ),
            ..._content(textTheme),
            SliverToBoxAdapter(child: SizedBox(height: 96)),
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
            padding: EdgeInsets.all(24),
            child: Column(children: [
              Text(_error!, textAlign: TextAlign.center),
              SizedBox(height: 12),
              OutlinedButton(onPressed: _load, child: Text('Try again')),
            ]),
          ),
        ),
      ];
    }
    if (_items == null) {
      return [
        SliverToBoxAdapter(
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
            padding: EdgeInsets.all(24),
            child: Column(children: [
              Icon(Icons.menu_book_outlined, size: 56, color: context.pal.muted),
              SizedBox(height: 12),
              Text(_query.isEmpty ? 'No books listed yet' : 'No books match your search',
                  textAlign: TextAlign.center, style: textTheme.titleMedium),
              SizedBox(height: 6),
              Text(
                _query.isEmpty ? 'Be the first to list one and start the chain.' : 'Try a different title or author.',
                textAlign: TextAlign.center,
                style: TextStyle(color: context.pal.muted),
              ),
              if (_query.trim().length >= 3) ...[
                SizedBox(height: 16),
                FilledButton.tonalIcon(
                  onPressed: _alertMe,
                  icon: Icon(Icons.notifications_active_outlined),
                  label: Text('Tell me when it\'s listed'),
                ),
              ],
            ]),
          ),
        ),
      ];
    }
    return [
      SliverPadding(
        padding: EdgeInsets.symmetric(horizontal: 20),
        sliver: SliverGrid.builder(
          gridDelegate: SliverGridDelegateWithMaxCrossAxisExtent(
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
  _BookCard({required this.book, required this.books, required this.onTap});

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
          SizedBox(height: 8),
          Text(book.title, maxLines: 1, overflow: TextOverflow.ellipsis,
              style: TextStyle(fontWeight: FontWeight.w600)),
          Text(
            [if (book.author != null) book.author!, book.condition.label].join(' · '),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 12, color: context.pal.muted),
          ),
          SizedBox(height: 2),
          Row(children: [
            Text('₹${book.price}', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 15)),
            if (book.mrp != null) ...[
              SizedBox(width: 6),
              Text('₹${book.mrp}',
                  style: TextStyle(
                      fontSize: 12, color: context.pal.muted, decoration: TextDecoration.lineThrough)),
            ],
            Spacer(),
            Flexible(
              child: Text(book.city,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 11, color: context.pal.primary, fontWeight: FontWeight.w600)),
            ),
          ]),
        ],
      ),
    );
  }
}


class _FiltersSheet extends StatefulWidget {
  const _FiltersSheet({required this.books, required this.initial});

  final BooksApi books;
  final BrowseFilters initial;

  @override
  State<_FiltersSheet> createState() => _FiltersSheetState();
}

class _FiltersSheetState extends State<_FiltersSheet> {
  static const _priceCaps = [100, 200, 300, 500];

  late String? _city = widget.initial.city;
  late BookCondition? _condition = widget.initial.condition;
  late int? _maxPrice = widget.initial.maxPrice;
  late BookSort _sort = widget.initial.sort;
  List<String>? _cities;

  @override
  void initState() {
    super.initState();
    widget.books.cities().then((c) {
      if (mounted) setState(() => _cities = c);
    }).catchError((Object _) {
      if (mounted) setState(() => _cities = const []);
    });
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(top: 16, bottom: 8),
        child: Text(text, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: context.pal.muted)),
      );

  @override
  Widget build(BuildContext context) {
    final cities = _cities;
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('Filters', style: Theme.of(context).textTheme.titleLarge),
          _label('City'),
          if (cities == null)
            const LinearProgressIndicator()
          else
            Wrap(spacing: 8, runSpacing: 8, children: [
              ChoiceChip(label: const Text('Anywhere'), selected: _city == null, onSelected: (_) => setState(() => _city = null)),
              for (final c in cities)
                ChoiceChip(label: Text(c), selected: _city == c, onSelected: (_) => setState(() => _city = c)),
            ]),
          _label('Condition'),
          Wrap(spacing: 8, runSpacing: 8, children: [
            ChoiceChip(label: const Text('Any'), selected: _condition == null, onSelected: (_) => setState(() => _condition = null)),
            for (final c in BookCondition.values)
              ChoiceChip(label: Text(c.label), selected: _condition == c, onSelected: (_) => setState(() => _condition = c)),
          ]),
          _label('Price'),
          Wrap(spacing: 8, runSpacing: 8, children: [
            ChoiceChip(label: const Text('Any'), selected: _maxPrice == null, onSelected: (_) => setState(() => _maxPrice = null)),
            for (final p in _priceCaps)
              ChoiceChip(label: Text('Under ₹$p'), selected: _maxPrice == p, onSelected: (_) => setState(() => _maxPrice = p)),
          ]),
          _label('Sort by'),
          Wrap(spacing: 8, runSpacing: 8, children: [
            for (final s in BookSort.values)
              ChoiceChip(label: Text(s.label), selected: _sort == s, onSelected: (_) => setState(() => _sort = s)),
          ]),
          const SizedBox(height: 24),
          Row(children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () => Navigator.of(context).pop(const BrowseFilters()),
                style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                child: const Text('Clear all'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: FilledButton(
                onPressed: () => Navigator.of(context).pop(
                  BrowseFilters(city: _city, condition: _condition, maxPrice: _maxPrice, sort: _sort),
                ),
                child: const Text('Show books'),
              ),
            ),
          ]),
        ]),
      ),
    );
  }
}


/// Stays on Home while any order is waiting for me.
class _AttentionBanner extends StatelessWidget {
  const _AttentionBanner({required this.orders, required this.onTap});

  final List<OrderView> orders;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final one = orders.length == 1;
    return Material(
      color: context.pal.primary,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(children: [
            const Text('📦', style: TextStyle(fontSize: 28)),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(
                  one ? 'An order needs you' : '${orders.length} orders need you',
                  style: TextStyle(color: context.pal.onPrimary, fontWeight: FontWeight.w700, fontSize: 16),
                ),
                const SizedBox(height: 4),
                Text(
                  one ? orders.first.nextStep : 'Tap to see them in My orders.',
                  style: TextStyle(color: context.pal.onPrimary),
                ),
              ]),
            ),
            Icon(Icons.chevron_right, color: context.pal.onPrimary),
          ]),
        ),
      ),
    );
  }
}
