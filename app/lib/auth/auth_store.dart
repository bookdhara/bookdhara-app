import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/api_client.dart';

class AppUser {
  AppUser({required this.id, required this.phone, this.name});

  factory AppUser.fromJson(Map<String, dynamic> json) => AppUser(
        id: (json['id'] as num).toInt(),
        phone: json['phone'] as String,
        name: json['name'] as String?,
      );

  final int id;
  final String phone;
  final String? name;

  bool get needsProfile => name == null || name!.trim().isEmpty;
}

class OtpSent {
  OtpSent({required this.phone, this.devCode});

  final String phone;

  /// Filled only while the backend has no SMS provider (test mode).
  final String? devCode;
}

/// Holds who is logged in, and remembers the login between visits.
class AuthStore extends ChangeNotifier {
  AuthStore(this.api);

  static const _tokenKey = 'auth_token';

  final ApiClient api;

  bool loaded = false;
  AppUser? user;

  bool get isLoggedIn => user != null;

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_tokenKey);
    if (saved != null) {
      api.token = saved;
      try {
        user = AppUser.fromJson(await api.get('/api/me'));
      } on ApiException catch (e) {
        if (e.isUnauthorized) {
          await _clear(prefs);
        }
      }
    }
    loaded = true;
    notifyListeners();
  }

  Future<OtpSent> requestOtp(String phone) async {
    final data = await api.post('/api/auth/otp/request', {'phone': phone});
    return OtpSent(phone: data['phone'] as String, devCode: data['devCode'] as String?);
  }

  Future<void> verifyOtp(String phone, String otp) async {
    final data = await api.post('/api/auth/otp/verify', {'phone': phone, 'otp': otp});
    final token = data['accessToken'] as String;
    api.token = token;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
    user = AppUser.fromJson(data['user'] as Map<String, dynamic>);
    notifyListeners();
  }

  Future<void> saveName(String name) async {
    user = AppUser.fromJson(await api.patch('/api/me', {'name': name.trim()}));
    notifyListeners();
  }

  Future<void> logout() async {
    await _clear(await SharedPreferences.getInstance());
    notifyListeners();
  }

  Future<void> _clear(SharedPreferences prefs) async {
    await prefs.remove(_tokenKey);
    api.token = null;
    user = null;
  }
}
