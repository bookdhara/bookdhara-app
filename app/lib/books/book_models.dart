enum BookCondition {
  likeNew('LIKE_NEW', 'Like new'),
  good('GOOD', 'Good'),
  readable('READABLE', 'Readable');

  const BookCondition(this.api, this.label);

  final String api;
  final String label;

  static BookCondition fromApi(String value) =>
      BookCondition.values.firstWhere((c) => c.api == value, orElse: () => BookCondition.good);
}

class BookSummary {
  BookSummary({
    this.status = 'AVAILABLE',
    required this.id,
    required this.title,
    this.author,
    required this.price,
    this.mrp,
    required this.condition,
    required this.city,
    this.coverPhotoUrl,
  });

  factory BookSummary.fromJson(Map<String, dynamic> j) => BookSummary(
        id: (j['id'] as num).toInt(),
        title: j['title'] as String,
        author: j['author'] as String?,
        price: (j['price'] as num).toInt(),
        mrp: (j['mrp'] as num?)?.toInt(),
        condition: BookCondition.fromApi(j['condition'] as String),
        city: j['city'] as String,
        coverPhotoUrl: j['coverPhotoUrl'] as String?,
        status: j['status'] as String? ?? 'AVAILABLE',
      );

  final int id;
  final String title;
  final String? author;
  final int price;
  final int? mrp;
  final BookCondition condition;
  final String city;
  final String? coverPhotoUrl;
  final String status;

  bool get isSold => status != 'AVAILABLE';
}

class BookDetail {
  BookDetail({
    required this.id,
    this.isbn,
    required this.title,
    this.author,
    required this.price,
    this.mrp,
    required this.condition,
    this.note,
    required this.city,
    required this.allowsPickup,
    required this.allowsShipping,
    required this.status,
    required this.photoUrls,
    required this.sellerId,
    this.sellerName,
    required this.mine,
    this.payout,
    this.payoutAmount,
    this.saved = false,
  });

  factory BookDetail.fromJson(Map<String, dynamic> j) {
    final seller = j['seller'] as Map<String, dynamic>;
    return BookDetail(
      id: (j['id'] as num).toInt(),
      isbn: j['isbn'] as String?,
      title: j['title'] as String,
      author: j['author'] as String?,
      price: (j['price'] as num).toInt(),
      mrp: (j['mrp'] as num?)?.toInt(),
      condition: BookCondition.fromApi(j['condition'] as String),
      note: j['note'] as String?,
      city: j['city'] as String,
      allowsPickup: j['allowsPickup'] as bool,
      allowsShipping: j['allowsShipping'] as bool,
      status: j['status'] as String,
      photoUrls: (j['photoUrls'] as List<dynamic>).cast<String>(),
      sellerId: (seller['id'] as num).toInt(),
      sellerName: seller['name'] as String?,
      mine: j['mine'] as bool,
      payout: j['payout'] as String?,
      payoutAmount: (j['payoutAmount'] as num?)?.toInt(),
      saved: j['saved'] as bool? ?? false,
    );
  }

  final int id;
  final String? isbn;
  final String title;
  final String? author;
  final int price;
  final int? mrp;
  final BookCondition condition;
  final String? note;
  final String city;
  final bool allowsPickup;
  final bool allowsShipping;
  final String status;
  final List<String> photoUrls;
  final int sellerId;
  final String? sellerName;
  final bool mine;
  final String? payout;
  final int? payoutAmount;
  final bool saved;

  /// Link anyone can open; the app jumps straight to this book.
  String get shareLink => 'https://bookdhara.github.io/bookdhara-app/?book=$id';

  bool get isAvailable => status == 'AVAILABLE';
}

/// Percentage saved compared with MRP, or null when MRP is unknown.
int? discountPercent(int price, int? mrp) {
  if (mrp == null || mrp <= 0 || price >= mrp) return null;
  return ((1 - price / mrp) * 100).round();
}

/// Credits get a 25% bonus over the cash price (same rule as the backend).
int creditsFor(int price) => (price * 1.25).round();
