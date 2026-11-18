import 'dart:convert';

import 'package:http/http.dart' as http;

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

  /// The free server can take up to a minute to wake up, so allow a long wait.
  static const _timeout = Duration(seconds: 90);

  String? token;

  Future<Map<String, dynamic>> get(String path) => _send('GET', path);

  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) => _send('POST', path, body);

  Future<Map<String, dynamic>> patch(String path, Map<String, dynamic> body) => _send('PATCH', path, body);

  Future<Map<String, dynamic>> _send(String method, String path, [Map<String, dynamic>? body]) async {
    final request = http.Request(method, Uri.parse('$baseUrl$path'));
    request.headers['Accept'] = 'application/json';
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }
    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }

    final http.Response response;
    try {
      response = await http.Response.fromStream(await _http.send(request).timeout(_timeout));
    } catch (_) {
      throw ApiException(0, 'NETWORK', 'Could not reach BookDhara. Check your internet and try again.');
    }

    final decoded = response.body.isEmpty ? <String, dynamic>{} : jsonDecode(response.body);
    final data = decoded is Map<String, dynamic> ? decoded : <String, dynamic>{};

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
