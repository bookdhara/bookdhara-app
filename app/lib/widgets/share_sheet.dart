import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import '../books/book_models.dart';

/// Share a listing: WhatsApp, or copy the link to paste anywhere (Instagram, Telegram…).
Future<void> showShareSheet(BuildContext context, BookDetail book) {
  final message = '📚 "${book.title}"${book.author == null ? '' : ' by ${book.author}'} for just ₹${book.price} '
      'on BookDhara (${book.city}).\n${book.shareLink}';
  return showModalBottomSheet<void>(
    context: context,
    builder: (ctx) => SafeArea(
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        const SizedBox(height: 8),
        ListTile(
          leading: const Icon(Icons.chat),
          title: const Text('Share on WhatsApp'),
          onTap: () async {
            Navigator.pop(ctx);
            final uri = Uri.parse('https://wa.me/?text=${Uri.encodeComponent(message)}');
            await launchUrl(uri, mode: LaunchMode.externalApplication);
          },
        ),
        ListTile(
          leading: const Icon(Icons.link),
          title: const Text('Copy link'),
          onTap: () async {
            await Clipboard.setData(ClipboardData(text: message));
            if (!ctx.mounted) return;
            Navigator.pop(ctx);
            ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Link copied. Paste it anywhere.')));
          },
        ),
        const SizedBox(height: 8),
      ]),
    ),
  );
}
