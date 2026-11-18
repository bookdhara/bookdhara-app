import 'package:flutter/material.dart';

import '../auth/auth_store.dart';
import '../theme.dart';

/// Home, following the design. Book listings come in the next feature, so for now
/// it shows an honest empty state instead of sample books.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key, required this.auth});

  final AuthStore auth;

  void _comingSoon(BuildContext context, String what) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$what is coming in the next update')));
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final firstName = (auth.user?.name ?? '').split(' ').first;

    return Scaffold(
      appBar: AppBar(
        titleSpacing: 20,
        title: Text('BookDhara', style: textTheme.titleLarge?.copyWith(color: AppColors.primary, fontSize: 24)),
        actions: [
          PopupMenuButton<String>(
            tooltip: 'Account',
            icon: const Icon(Icons.account_circle_outlined),
            onSelected: (value) {
              if (value == 'logout') auth.logout();
            },
            itemBuilder: (_) => [
              PopupMenuItem<String>(enabled: false, child: Text(auth.user?.phone ?? '')),
              const PopupMenuItem<String>(value: 'logout', child: Text('Log out')),
            ],
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text('Hi $firstName 👋', style: textTheme.titleLarge),
          const SizedBox(height: 16),
          TextField(
            readOnly: true,
            onTap: () => _comingSoon(context, 'Search'),
            decoration: const InputDecoration(
              hintText: 'Search title, author or ISBN',
              prefixIcon: Icon(Icons.search),
            ),
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.accentTint,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFFF4D9A8)),
            ),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(color: AppColors.accent, borderRadius: BorderRadius.circular(12)),
                  child: const Icon(Icons.add, color: AppColors.ink),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Read a book recently?', style: TextStyle(fontWeight: FontWeight.w600)),
                      SizedBox(height: 2),
                      Text('List it and earn credits',
                          style: TextStyle(fontSize: 12, color: AppColors.muted)),
                    ],
                  ),
                ),
                TextButton(onPressed: () => _comingSoon(context, 'Selling'), child: const Text('Sell')),
              ],
            ),
          ),
          const SizedBox(height: 32),
          const Icon(Icons.menu_book_outlined, size: 56, color: AppColors.muted),
          const SizedBox(height: 12),
          Text('No books near you yet', textAlign: TextAlign.center, style: textTheme.titleMedium),
          const SizedBox(height: 6),
          const Text(
            'Be the first to list one and start the chain.',
            textAlign: TextAlign.center,
            style: TextStyle(color: AppColors.muted),
          ),
        ],
      ),
    );
  }
}
