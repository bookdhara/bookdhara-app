import '../api/api_client.dart';
import 'book_models.dart';

class IsbnInfo {
  IsbnInfo({required this.isbn, required this.title, this.author});

  final String isbn;
  final String title;
  final String? author;
}

class NewBook {
  NewBook({
    this.isbn,
    required this.title,
    this.author,
    this.mrp,
    required this.price,
    required this.condition,
    this.note,
    required this.city,
    required this.pincode,
    required this.allowsPickup,
    required this.allowsShipping,
    required this.payoutCredits,
  });

  final String? isbn;
  final String title;
  final String? author;
  final int? mrp;
  final int price;
  final BookCondition condition;
  final String? note;
  final String city;
  final String pincode;
  final bool allowsPickup;
  final bool allowsShipping;
  final bool payoutCredits;

  Map<String, dynamic> toJson() => {
        'isbn': isbn,
        'title': title,
        'author': author,
        'mrp': mrp,
        'price': price,
        'condition': condition.api,
        'note': note,
        'city': city,
        'pincode': pincode,
        'allowsPickup': allowsPickup,
        'allowsShipping': allowsShipping,
        'payout': payoutCredits ? 'CREDITS' : 'CASH',
      };
}

class BooksApi {
  BooksApi(this.api);

  final ApiClient api;

  Future<List<BookSummary>> browse({String? query, String? city}) async {
    final params = <String, String>{
      if (query != null && query.trim().isNotEmpty) 'q': query.trim(),
      if (city != null && city.trim().isNotEmpty) 'city': city.trim(),
    };
    final path = Uri(path: '/api/books', queryParameters: params.isEmpty ? null : params).toString();
    final items = await api.getList(path);
    return items.map((e) => BookSummary.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<BookSummary>> myListings() async {
    final items = await api.getList('/api/me/books');
    return items.map((e) => BookSummary.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<BookDetail> detail(int id) async => BookDetail.fromJson(await api.get('/api/books/$id'));

  Future<IsbnInfo> lookupIsbn(String isbn) async {
    final data = await api.get('/api/isbn/${Uri.encodeComponent(isbn)}');
    return IsbnInfo(isbn: data['isbn'] as String, title: data['title'] as String, author: data['author'] as String?);
  }

  Future<BookDetail> create(NewBook book) async => BookDetail.fromJson(await api.post('/api/books', book.toJson()));

  Future<void> addPhoto(int bookId, List<int> bytes, String filename, String contentType) =>
      api.uploadFile('/api/books/$bookId/photos', bytes, filename, contentType);

  Future<void> remove(int bookId) => api.delete('/api/books/$bookId');

  String photoUrl(String path) => api.url(path);
}
