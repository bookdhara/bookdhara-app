/// Links like https://bookdhara.github.io/bookdhara-app/?book=42 open that book after login.
class DeepLinks {
  DeepLinks._();

  static int? _pendingBookId = int.tryParse(Uri.base.queryParameters['book'] ?? '');

  /// The linked book, once. Later calls return null.
  static int? takeBookId() {
    final id = _pendingBookId;
    _pendingBookId = null;
    return id;
  }
}
