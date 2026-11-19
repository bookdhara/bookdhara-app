import 'package:flutter/material.dart';

import '../theme.dart';

const _contactEmail = 'bookdhara.in@gmail.com';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return _InfoPage(
      title: 'About BookDhara',
      blocks: const [
        _Block.heading('Kitaabein behti rahein.'),
        _Block.text(
            'BookDhara helps a book you have finished reach its next reader. List the books on your shelf at a '
            'small price, and pick up books other readers have finished, near you or by courier.'),
        _Block.heading('How it works'),
        _Block.bullets([
          'List a book: add its ISBN, a few photos, the condition and your price.',
          'When it sells, choose cash or credits. Credits come with a 25% bonus and work like money on your next book.',
          'Buyers choose home delivery or local pickup. For pickup, the buyer shares a 4-digit code once they have the book, and only then is the seller paid.',
          'Payments are held safely until the buyer confirms the book arrived.',
        ]),
        _Block.heading('Who we are'),
        _Block.text(
            'BookDhara is a small independent project started in Mumbai by a reader and software developer, '
            'to keep good books in circulation instead of on dusty shelves.'),
        _Block.heading('Contact'),
        _Block.text('Questions, ideas or a problem with an order? Write to us at $_contactEmail.'),
        _Block.note('BookDhara is in early testing. Buying and payments are not open yet.'),
      ],
    );
  }
}

/// Plain-language draft terms. Have them reviewed before public launch.
class TermsScreen extends StatelessWidget {
  const TermsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return _InfoPage(
      title: 'Terms & Privacy',
      blocks: const [
        _Block.note('Draft, last updated 19 November 2026. BookDhara is in early testing and these terms may change.'),
        _Block.heading('1. Using BookDhara'),
        _Block.bullets([
          'You need a valid email address to use BookDhara, and you are responsible for activity on your account.',
          'Be respectful to other readers. We may suspend accounts that cheat, harass others or misuse the app.',
        ]),
        _Block.heading('2. Listing books'),
        _Block.bullets([
          'Only list physical books you own and can hand over.',
          'No pirated, photocopied or counterfeit books, and nothing illegal or offensive.',
          'Describe the condition honestly and use real photos of your copy.',
          'We may remove listings that break these rules.',
        ]),
        _Block.heading('3. Buying, credits and payments'),
        _Block.bullets([
          'Prices are set by sellers. Delivery charges and a small platform fee are shown before you pay.',
          'Your payment is held until you confirm the book arrived (or share your pickup code), and then released to the seller.',
          'Credits can be used only inside BookDhara to buy books. They cannot be exchanged for cash unless we say otherwise.',
          'If a book does not arrive or is very different from its listing, report it within 48 hours and we will help sort it out.',
        ]),
        _Block.heading('4. Local pickup'),
        _Block.bullets([
          'Meet in a public place, check the book before sharing your code, and never share the code before you have the book.',
          'BookDhara is not responsible for what happens at in-person meetings.',
        ]),
        _Block.heading('5. Privacy'),
        _Block.bullets([
          'We store your email, name, optional mobile number, your listings and their photos, and your orders.',
          'Other users see your name, your listings and your city, never your email or phone, except what is needed to deliver an order you place or accept.',
          'We use your email to send login codes and order updates. We do not sell your data.',
          'To delete your account and data, write to $_contactEmail.',
        ]),
        _Block.heading('6. Our responsibility'),
        _Block.text(
            'BookDhara connects readers; the books are sold by users, not by BookDhara. We try to keep the app '
            'working and safe but cannot promise it will always be available or error-free.'),
        _Block.heading('Contact'),
        _Block.text('$_contactEmail'),
      ],
    );
  }
}

class _Block {
  const _Block.heading(this.text)
      : kind = 'h',
        items = const [];
  const _Block.text(this.text)
      : kind = 'p',
        items = const [];
  const _Block.note(this.text)
      : kind = 'note',
        items = const [];
  const _Block.bullets(this.items)
      : kind = 'ul',
        text = '';

  final String kind;
  final String text;
  final List<String> items;
}

class _InfoPage extends StatelessWidget {
  const _InfoPage({required this.title, required this.blocks});

  final String title;
  final List<_Block> blocks;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 640),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 40),
            children: [
              for (final b in blocks)
                switch (b.kind) {
                  'h' => Padding(
                      padding: const EdgeInsets.only(top: 20, bottom: 8),
                      child: Text(b.text, style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
                    ),
                  'note' => Container(
                      margin: const EdgeInsets.only(top: 12),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: context.pal.accentTint,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: context.pal.accentBorder),
                      ),
                      child: Text(b.text, style: const TextStyle(height: 1.4)),
                    ),
                  'ul' => Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        for (final item in b.items)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              const Text('•  ', style: TextStyle(height: 1.45)),
                              Expanded(child: Text(item, style: const TextStyle(height: 1.45))),
                            ]),
                          ),
                      ],
                    ),
                  _ => Text(b.text, style: const TextStyle(height: 1.45)),
                },
            ],
          ),
        ),
      ),
    );
  }
}
