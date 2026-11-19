import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/api_client.dart';

class AppUser {
  AppUser({required this.id, this.email, this.phone, this.name});

  factory AppUser.fromJson(Map<String, dynamic> json) => AppUser(
        id: (json['id'] as num).toInt(),
        email: json['email'] as String?,
        phone: json['phone'] as String?,
        name: json['name'] as String?,
      );

  final int id;
  final String? email;
  final String? phone;
  final String? name;

  Map<String, dynamic> toJson() => {'id': id, 'email': email, 'phone': phone, 'name': name};

  bool get needsProfile => name == null || name!.trim().isEmpty;
}

class OtpSent {
  OtpSent({required this.email, this.devCode});

  final String email;

  /// Filled only while the backend has no email service connected (test mode).
  final String? devCode;
}

/// Holds who is logged in, and remembers the login between visits.
class AuthStore extends ChangeNotifier {
  AuthStore(this.api);

  static const _tokenKey = 'auth_token';
  static const _userKey = 'auth_user';

  final ApiClient api;

  bool loaded = false;
  AppUser? user;

  bool get isLoggedIn => user != null;

  /// Opens the app straight away from the saved login, then refreshes it from the server in the
  /// background (the free server may take a minute to wake up, and nobody should stare at a spinner).
  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_tokenKey);
    final cachedUser = prefs.getString(_userKey);
    if (saved != null) {
      api.token = saved;
      if (cachedUser != null) {
        user = _usable(AppUser.fromJson(jsonDecode(cachedUser) as Map<String, dynamic>));
        if (user == null) await _clear(prefs);
      }
    }
    loaded = true;
    notifyListeners();

    if (api.token != null) {
      try {
        user = _usable(AppUser.fromJson(await api.get('/api/me')));
        if (user == null) {
          await _clear(prefs);
        } else {
          await prefs.setString(_userKey, jsonEncode(user!.toJson()));
        }
      } on ApiException catch (e) {
        if (e.isUnauthorized) await _clear(prefs);
      }
      notifyListeners();
    }
  }

  /// Accounts from the old phone login have no email; ask them to log in again with email.
  AppUser? _usable(AppUser u) => u.email == null ? null : u;

  Future<OtpSent> requestOtp(String email) async {
    final data = await api.post('/api/auth/otp/request', {'email': email.trim()});
    return OtpSent(email: data['email'] as String, devCode: data['devCode'] as String?);
  }

  Future<void> verifyOtp(String email, String otp) async {
    final data = await api.post('/api/auth/otp/verify', {'email': email, 'otp': otp});
    final token = data['accessToken'] as String;
    api.token = token;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
    user = AppUser.fromJson(data['user'] as Map<String, dynamic>);
    await prefs.setString(_userKey, jsonEncode(user!.toJson()));
    notifyListeners();
  }

  Future<void> saveName(String name) => saveProfile(name: name);

  /// Updates the name, and the phone too when given ('' removes it).
  Future<void> saveProfile({required String name, String? phone}) async {
    user = AppUser.fromJson(await api.patch('/api/me', {'name': name.trim(), if (phone != null) 'phone': phone}));
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_userKey, jsonEncode(user!.toJson()));
    notifyListeners();
  }

  Future<void> logout() async {
    await _clear(await SharedPreferences.getInstance());
    notifyListeners();
  }

  Future<void> _clear(SharedPreferences prefs) async {
    await prefs.remove(_tokenKey);
    await prefs.remove(_userKey);
    api.token = null;
    user = null;
  }
}
