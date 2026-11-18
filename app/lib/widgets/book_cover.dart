import 'package:flutter/material.dart';

import '../theme.dart';

/// A book's photo, or a coloured block with its title when there is no photo.
class BookCover extends StatelessWidget {
  const BookCover({super.key, required this.title, this.imageUrl, this.height = 150, this.radius = 14});

  final String title;
  final String? imageUrl;
  final double height;
  final double radius;

  static const _colors = [
    Color(0xFF1F4E5F),
    Color(0xFFB5562E),
    Color(0xFF4B3F72),
    Color(0xFF2E6B3A),
    Color(0xFF7A4A1F),
  ];

  @override
  Widget build(BuildContext context) {
    final placeholder = Container(
      height: height,
      width: double.infinity,
      color: _colors[title.hashCode.abs() % _colors.length],
      alignment: Alignment.bottomLeft,
      padding: const EdgeInsets.all(12),
      child: Text(
        title,
        maxLines: 3,
        overflow: TextOverflow.ellipsis,
        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 15, height: 1.2),
      ),
    );

    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: imageUrl == null
          ? placeholder
          : Image.network(
              imageUrl!,
              height: height,
              width: double.infinity,
              fit: BoxFit.cover,
              semanticLabel: 'Photo of $title',
              errorBuilder: (_, __, ___) => placeholder,
              loadingBuilder: (context, child, progress) => progress == null
                  ? child
                  : Container(height: height, color: AppColors.border),
            ),
    );
  }
}
