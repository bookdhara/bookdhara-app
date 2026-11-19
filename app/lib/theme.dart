import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Brand colours that look the same in light and dark mode.
class AppColors {
  static const primary = Color(0xFF0B6B5D);
  static const accent = Color(0xFFF2A93B);
}

/// Colours that change with light/dark mode. Use them through `context.pal`.
class Palette {
  const Palette({
    required this.ground,
    required this.surface,
    required this.ink,
    required this.muted,
    required this.border,
    required this.primary,
    required this.onPrimary,
    required this.primaryTint,
    required this.accentTint,
    required this.accentBorder,
    required this.danger,
  });

  final Color ground;
  final Color surface;
  final Color ink;
  final Color muted;
  final Color border;

  /// Primary colour for text, icons and buttons on this background (lighter teal in dark mode).
  final Color primary;
  final Color onPrimary;
  final Color primaryTint;
  final Color accentTint;
  final Color accentBorder;
  final Color danger;
}

const lightPalette = Palette(
  ground: Color(0xFFF5F6F2),
  surface: Colors.white,
  ink: Color(0xFF15201E),
  muted: Color(0xFF5B6763),
  border: Color(0xFFE2E5E0),
  primary: AppColors.primary,
  onPrimary: Colors.white,
  primaryTint: Color(0xFFE3F1EE),
  accentTint: Color(0xFFFFF4E0),
  accentBorder: Color(0xFFF4D9A8),
  danger: Color(0xFFA23B2A),
);

const darkPalette = Palette(
  ground: Color(0xFF101614),
  surface: Color(0xFF1A2220),
  ink: Color(0xFFE8EEEC),
  muted: Color(0xFFA3B0AC),
  border: Color(0xFF2E3936),
  primary: Color(0xFF5CC5B3),
  onPrimary: Color(0xFF06241F),
  primaryTint: Color(0xFF173A34),
  accentTint: Color(0xFF3A2D14),
  accentBorder: Color(0xFF5C4620),
  danger: Color(0xFFFF8A75),
);

extension PaletteContext on BuildContext {
  Palette get pal => Theme.of(this).brightness == Brightness.dark ? darkPalette : lightPalette;
}

ThemeData buildTheme(Brightness brightness) {
  final p = brightness == Brightness.dark ? darkPalette : lightPalette;
  final base = ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      brightness: brightness,
      primary: p.primary,
      onPrimary: p.onPrimary,
      surface: p.surface,
      onSurface: p.ink,
      error: p.danger,
    ),
    scaffoldBackgroundColor: p.ground,
  );

  final text = GoogleFonts.interTextTheme(base.textTheme).apply(bodyColor: p.ink, displayColor: p.ink);
  final border = OutlineInputBorder(
    borderRadius: BorderRadius.circular(14),
    borderSide: BorderSide(color: p.border),
  );

  return base.copyWith(
    textTheme: text.copyWith(
      headlineMedium: GoogleFonts.spaceGrotesk(fontSize: 26, fontWeight: FontWeight.w700, color: p.ink),
      titleLarge: GoogleFonts.spaceGrotesk(fontSize: 20, fontWeight: FontWeight.w600, color: p.ink),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: p.surface,
      foregroundColor: p.ink,
      elevation: 0,
      scrolledUnderElevation: 0,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: p.surface,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      border: border,
      enabledBorder: border,
      focusedBorder: border.copyWith(borderSide: BorderSide(color: p.primary, width: 2)),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: p.primary,
        foregroundColor: p.onPrimary,
        minimumSize: const Size.fromHeight(52),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        textStyle: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.w600),
      ),
    ),
    dividerTheme: DividerThemeData(color: p.border),
  );
}
