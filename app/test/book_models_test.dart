import 'package:bookdhara/books/book_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('discount is shown only when the price is below MRP', () {
    expect(discountPercent(160, 399), 60);
    expect(discountPercent(160, null), isNull);
    expect(discountPercent(400, 399), isNull);
  });

  test('credits carry a 25% bonus', () {
    expect(creditsFor(160), 200);
    expect(creditsFor(99), 124);
  });

  test('book summary reads the API shape', () {
    final book = BookSummary.fromJson({
      'id': 7,
      'title': 'Atomic Habits',
      'author': 'James Clear',
      'price': 160,
      'mrp': 399,
      'condition': 'LIKE_NEW',
      'city': 'Mumbai',
      'coverPhotoUrl': '/api/photos/3',
    });
    expect(book.condition, BookCondition.likeNew);
    expect(book.coverPhotoUrl, '/api/photos/3');
  });
}
