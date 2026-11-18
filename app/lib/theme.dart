import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Colours from the BookDhara screen designs.
class AppColors {
  static const primary = Color(0xFF0B6B5D);
  static const primaryTint = Color(0xFFE3F1EE);
  static const accent = Color(0xFFF2A93B);
  static const accentTint = Color(0xFFFFF4E0);
  static const ground = Color(0xFFF5F6F2);
  static const surface = Colors.white;
  static const ink = Color(0xFF15201E);
  static const muted = Color(0xFF5B6763);
  static const border = Color(0xFFE2E5E0);
  static const danger = Color(0xFFA23B2A);
}

ThemeData buildTheme() {
  final base = ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      primary: AppColors.primary,
      surface: AppColors.surface,
      error: AppColors.danger,
    ),
    scaffoldBackgroundColor: AppColors.ground,
  );

  final text = GoogleFonts.interTextTheme(base.textTheme).apply(
    bodyColor: AppColors.ink,
    displayColor: AppColors.ink,
  );

  return base.copyWith(
    textTheme: text.copyWith(
      headlineMedium: GoogleFonts.spaceGrotesk(fontSize: 26, fontWeight: FontWeight.w700, color: AppColors.ink),
      titleLarge: GoogleFonts.spaceGrotesk(fontSize: 20, fontWeight: FontWeight.w600, color: AppColors.ink),
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.surface,
      foregroundColor: AppColors.ink,
      elevation: 0,
      scrolledUnderElevation: 0,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: AppColors.surface,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.border),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.border),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.primary, width: 2),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        minimumSize: const Size.fromHeight(52),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        textStyle: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.w600),
      ),
    ),
  );
}
