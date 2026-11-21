import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

import '../config.dart';

/// An error the backend sent back, with a message we can show to the user.
class ApiException implements Exception {
  ApiException(this.statusCode, this.code, this.message);

  final int statusCode;
  final String code;
  final String message;

  bool get isUnauthorized => statusCode == 401;

  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({http.Client? httpClient, this.baseUrl = apiBase}) : _http = httpClient ?? http.Client();

  final http.Client _http;
  final String baseUrl;

  /// The free server can take up to a minute to wake up. While it wakes, Render answers with its own
  /// loading page, which the browser reports as a network error, so we keep retrying for a while.
  static const _attemptTimeout = Duration(seconds: 45);
  static const _wakeUpWindow = Duration(seconds: 100);
  static const _retryDelay = Duration(seconds: 4);

  String? token;

  Future<Map<String, dynamic>> get(String path) => _send('GET', path);

  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) => _send('POST', path, body);

  Future<Map<String, dynamic>> patch(String path, Map<String, dynamic> body) => _send('PATCH', path, body);

  Future<Map<String, dynamic>> delete(String path) => _send('DELETE', path);

  Future<Map<String, dynamic>> put(String path, [Map<String, dynamic>? body]) => _send('PUT', path, body);

  Future<List<dynamic>> getList(String path) async {
    final data = await _send('GET', path);
    return data['items'] as List<dynamic>? ?? const [];
  }

  /// Full address for a path the backend returns, such as a photo url.
  String url(String path) => '$baseUrl$path';

  /// Pings the server so a sleeping free server starts waking up. Errors are ignored.
  Future<void> wakeUp() async {
    try {
      await _execute(() => http.Request('GET', Uri.parse('$baseUrl/api/status')));
    } catch (_) {
      // Best effort only.
    }
  }

  Future<Map<String, dynamic>> uploadFile(String path, List<int> bytes, String filename, String contentType) {
    return _execute(() => http.MultipartRequest('POST', Uri.parse('$baseUrl$path'))
      ..files.add(http.MultipartFile.fromBytes('file', bytes,
          filename: filename, contentType: MediaType.parse(contentType))));
  }

  Future<Map<String, dynamic>> _send(String method, String path, [Map<String, dynamic>? body]) {
    return _execute(() {
      final request = http.Request(method, Uri.parse('$baseUrl$path'));
      if (body != null) {
        request.headers['Content-Type'] = 'application/json';
        request.body = jsonEncode(body);
      }
      return request;
    });
  }

  /// Sends a request (built fresh for every try) and retries network errors while the server wakes up.
  static bool _looksLikeJson(http.Response r) {
    final type = r.headers['content-type'] ?? '';
    return type.contains('json');
  }

  Future<Map<String, dynamic>> _execute(http.BaseRequest Function() build) async {
    final deadline = DateTime.now().add(_wakeUpWindow);
    http.Response? response;
    while (response == null) {
      final request = build();
      request.headers['Accept'] = 'application/json';
      if (token != null) {
        request.headers['Authorization'] = 'Bearer $token';
      }
      try {
        final r = await http.Response.fromStream(await _http.send(request).timeout(_attemptTimeout));
        // While waking up, Render can answer with its own HTML error page (502/503): treat that like
        // "not reachable yet" and try again instead of failing.
        if (r.statusCode >= 500 && !_looksLikeJson(r)) throw StateError('server waking up');
        response = r;
      } catch (_) {
        if (DateTime.now().add(_retryDelay).isAfter(deadline)) {
          throw ApiException(0, 'NETWORK', 'Could not reach BookDhara. Check your internet and try again.');
        }
        await Future<void>.delayed(_retryDelay);
      }
    }

    dynamic decoded;
    try {
      decoded = response.body.isEmpty ? <String, dynamic>{} : jsonDecode(response.body);
    } on FormatException {
      decoded = <String, dynamic>{};
    }
    // Lists come back wrapped as {"items": [...]} so every call returns a map.
    final data = decoded is Map<String, dynamic>
        ? decoded
        : decoded is List
            ? <String, dynamic>{'items': decoded}
            : <String, dynamic>{};

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return data;
    }
    if (response.statusCode == 401) {
      throw ApiException(401, 'UNAUTHORIZED', 'Please log in again.');
    }
    throw ApiException(
      response.statusCode,
      data['code'] as String? ?? 'ERROR',
      data['message'] as String? ?? 'Something went wrong. Please try again.',
    );
  }
}
