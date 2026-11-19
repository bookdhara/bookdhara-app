import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// App preferences kept on the device: for now, light / dark / system appearance.
class AppSettings extends ChangeNotifier {
  static const _themeKey = 'theme_mode';

  ThemeMode themeMode = ThemeMode.system;

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    themeMode = ThemeMode.values.firstWhere(
      (m) => m.name == prefs.getString(_themeKey),
      orElse: () => ThemeMode.system,
    );
    notifyListeners();
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    themeMode = mode;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_themeKey, mode.name);
  }
}
