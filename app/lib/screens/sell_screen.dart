import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

import '../api/api_client.dart';
import '../books/book_models.dart';
import '../books/books_api.dart';
import '../theme.dart';
import '../widgets/busy_button.dart';

class _PickedPhoto {
  _PickedPhoto(this.bytes, this.name, this.contentType);

  final Uint8List bytes;
  final String name;
  final String contentType;
}

/// "List a book": ISBN lookup, photos, condition, price, delivery and payout choice.
class SellScreen extends StatefulWidget {
  const SellScreen({super.key, required this.books});

  final BooksApi books;

  @override
  State<SellScreen> createState() => _SellScreenState();
}

class _SellScreenState extends State<SellScreen> {
  final _form = GlobalKey<FormState>();
  final _isbn = TextEditingController();
  final _title = TextEditingController();
  final _author = TextEditingController();
  final _mrp = TextEditingController();
  final _price = TextEditingController();
  final _note = TextEditingController();
  final _city = TextEditingController();
  final _pincode = TextEditingController();

  final List<_PickedPhoto> _photos = [];
  BookCondition _condition = BookCondition.good;
  bool _pickup = true;
  bool _shipping = true;
  bool _credits = true;
  bool _lookingUp = false;
  bool _saving = false;
  String? _isbnMessage;
  String? _error;

  @override
  void initState() {
    super.initState();
    _price.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    for (final c in [_isbn, _title, _author, _mrp, _price, _note, _city, _pincode]) {
      c.dispose();
    }
    super.dispose();
  }

  int? get _priceValue => int.tryParse(_price.text);

  Future<void> _lookup() async {
    final isbn = _isbn.text.trim();
    if (isbn.isEmpty) return;
    setState(() {
      _lookingUp = true;
      _isbnMessage = null;
    });
    try {
      final info = await widget.books.lookupIsbn(isbn);
      setState(() {
        _title.text = info.title;
        _author.text = info.author ?? '';
        _isbnMessage = 'Found it! Check the details below.';
      });
    } on ApiException catch (e) {
      setState(() => _isbnMessage = e.message);
    } finally {
      if (mounted) setState(() => _lookingUp = false);
    }
  }

  Future<void> _addPhoto() async {
    final file = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 1200, imageQuality: 80);
    if (file == null) return;
    final bytes = await file.readAsBytes();
    if (bytes.length > 2 * 1024 * 1024) {
      setState(() => _error = 'That photo is too large. Please pick a smaller one.');
      return;
    }
    setState(() => _photos.add(_PickedPhoto(bytes, file.name, _contentType(file))));
  }

  String _contentType(XFile file) {
    final type = file.mimeType;
    if (type != null && type.startsWith('image/')) return type;
    final name = file.name.toLowerCase();
    if (name.endsWith('.png')) return 'image/png';
    if (name.endsWith('.webp')) return 'image/webp';
    return 'image/jpeg';
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_form.currentState!.validate()) return;
    if (!_pickup && !_shipping) {
      setState(() => _error = 'Choose local pickup, delivery, or both.');
      return;
    }
    setState(() => _saving = true);
    try {
      final book = await widget.books.create(NewBook(
        isbn: _isbn.text.trim().isEmpty ? null : _isbn.text.trim(),
        title: _title.text.trim(),
        author: _author.text.trim().isEmpty ? null : _author.text.trim(),
        mrp: int.tryParse(_mrp.text),
        price: _priceValue!,
        condition: _condition,
        note: _note.text.trim().isEmpty ? null : _note.text.trim(),
        city: _city.text.trim(),
        pincode: _pincode.text.trim(),
        allowsPickup: _pickup,
        allowsShipping: _shipping,
        payoutCredits: _credits,
      ));
      for (final photo in _photos) {
        await widget.books.addPhoto(book.id, photo.bytes, photo.name, photo.contentType);
      }
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  String? _required(String? v, String what) => v == null || v.trim().isEmpty ? 'Enter $what' : null;

  @override
  Widget build(BuildContext context) {
    final price = _priceValue;
    return Scaffold(
      appBar: AppBar(title: Text('List a book')),
      body: Form(
        key: _form,
        child: ListView(
          padding: EdgeInsets.all(20),
          children: [
            _section('ISBN (optional)'),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Expanded(
                child: TextFormField(
                  controller: _isbn,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9Xx\-\s]'))],
                  decoration: InputDecoration(
                    hintText: 'Back cover, above the barcode',
                  ),
                  onFieldSubmitted: (_) => _lookup(),
                ),
              ),
              SizedBox(width: 8),
              SizedBox(
                height: 54,
                child: OutlinedButton(
                  onPressed: _lookingUp ? null : _lookup,
                  child: _lookingUp
                      ? SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                      : Text('Find'),
                ),
              ),
            ]),
            if (_isbnMessage != null)
              Padding(
                padding: EdgeInsets.only(top: 6),
                child: Text(_isbnMessage!, style: TextStyle(fontSize: 13, color: context.pal.muted)),
              ),
            SizedBox(height: 20),
            _section('Book'),
            TextFormField(
              controller: _title,
              textCapitalization: TextCapitalization.words,
              decoration: InputDecoration(labelText: 'Title'),
              validator: (v) => _required(v, 'the title'),
            ),
            SizedBox(height: 12),
            TextFormField(
              controller: _author,
              textCapitalization: TextCapitalization.words,
              decoration: InputDecoration(labelText: 'Author (optional)'),
            ),
            SizedBox(height: 20),
            _section('Photos (up to 3)'),
            Wrap(spacing: 10, runSpacing: 10, children: [
              for (var i = 0; i < _photos.length; i++)
                Stack(children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: Image.memory(_photos[i].bytes, width: 90, height: 110, fit: BoxFit.cover),
                  ),
                  Positioned(
                    top: 2,
                    right: 2,
                    child: IconButton.filled(
                      tooltip: 'Remove photo',
                      iconSize: 16,
                      visualDensity: VisualDensity.compact,
                      style: IconButton.styleFrom(backgroundColor: Colors.black54),
                      onPressed: () => setState(() => _photos.removeAt(i)),
                      icon: Icon(Icons.close, color: Colors.white),
                    ),
                  ),
                ]),
              if (_photos.length < 3)
                SizedBox(
                  width: 90,
                  height: 110,
                  child: OutlinedButton(
                    onPressed: _addPhoto,
                    style: OutlinedButton.styleFrom(
                      padding: EdgeInsets.zero,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: Column(mainAxisSize: MainAxisSize.min, children: [
                      Icon(Icons.add_a_photo_outlined),
                      SizedBox(height: 4),
                      Text('Add', style: TextStyle(fontSize: 12)),
                    ]),
                  ),
                ),
            ]),
            SizedBox(height: 20),
            _section('Condition'),
            SegmentedButton<BookCondition>(
              segments: [
                for (final c in BookCondition.values) ButtonSegment(value: c, label: Text(c.label)),
              ],
              selected: {_condition},
              showSelectedIcon: false,
              onSelectionChanged: (s) => setState(() => _condition = s.first),
            ),
            SizedBox(height: 20),
            _section('Price'),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Expanded(
                child: TextFormField(
                  controller: _mrp,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  decoration: InputDecoration(labelText: 'MRP (optional)', prefixText: '₹ '),
                ),
              ),
              SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  controller: _price,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  decoration: InputDecoration(labelText: 'Your price', prefixText: '₹ '),
                  validator: (v) {
                    final n = int.tryParse(v ?? '');
                    if (n == null || n < 1) return 'Enter a price';
                    if (n > 100000) return 'Too high';
                    return null;
                  },
                ),
              ),
            ]),
            Padding(
              padding: EdgeInsets.only(top: 6),
              child: Text('Tip: used books sell fastest at 30–50% of MRP.',
                  style: TextStyle(fontSize: 12, color: context.pal.muted)),
            ),
            SizedBox(height: 20),
            _section('When it sells, I want'),
            Row(children: [
              Expanded(
                child: _ChoiceCard(
                  selected: !_credits,
                  title: 'Cash',
                  value: price == null ? '—' : '₹$price',
                  onTap: () => setState(() => _credits = false),
                ),
              ),
              SizedBox(width: 10),
              Expanded(
                child: _ChoiceCard(
                  selected: _credits,
                  title: 'Credits (+25%)',
                  value: price == null ? '—' : '${creditsFor(price)}',
                  onTap: () => setState(() => _credits = true),
                ),
              ),
            ]),
            SizedBox(height: 20),
            _section('How buyers can get it'),
            CheckboxListTile(
              value: _pickup,
              onChanged: (v) => setState(() => _pickup = v ?? false),
              title: Text('Local pickup'),
              subtitle: Text('Meet the buyer in your city'),
              contentPadding: EdgeInsets.zero,
              controlAffinity: ListTileControlAffinity.leading,
            ),
            CheckboxListTile(
              value: _shipping,
              onChanged: (v) => setState(() => _shipping = v ?? false),
              title: Text('Home delivery'),
              subtitle: Text('A courier picks it up from you'),
              contentPadding: EdgeInsets.zero,
              controlAffinity: ListTileControlAffinity.leading,
            ),
            SizedBox(height: 12),
            _section('Your location'),
            Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Expanded(
                flex: 3,
                child: TextFormField(
                  controller: _city,
                  textCapitalization: TextCapitalization.words,
                  decoration: InputDecoration(labelText: 'City'),
                  validator: (v) => _required(v, 'your city'),
                ),
              ),
              SizedBox(width: 12),
              Expanded(
                flex: 2,
                child: TextFormField(
                  controller: _pincode,
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  decoration: InputDecoration(labelText: 'Pincode', counterText: ''),
                  validator: (v) => RegExp(r'^\d{6}$').hasMatch(v ?? '') ? null : '6 digits',
                ),
              ),
            ]),
            SizedBox(height: 20),
            _section("Note for buyers (optional)"),
            TextFormField(
              controller: _note,
              maxLines: 3,
              maxLength: 500,
              decoration: InputDecoration(hintText: 'Any marks, missing pages, or why you loved it'),
            ),
            if (_error != null)
              Padding(
                padding: EdgeInsets.only(bottom: 12),
                child: Text(_error!, style: TextStyle(color: context.pal.danger)),
              ),
            BusyButton(label: 'List my book', busy: _saving, onPressed: _submit),
            SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _section(String text) => Padding(
        padding: EdgeInsets.only(bottom: 8),
        child: Text(text, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: context.pal.muted)),
      );
}

class _ChoiceCard extends StatelessWidget {
  _ChoiceCard({required this.selected, required this.title, required this.value, required this.onTap});

  final bool selected;
  final String title;
  final String value;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      selected: selected,
      button: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: selected ? context.pal.primaryTint : context.pal.surface,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: selected ? context.pal.primary : context.pal.border, width: selected ? 2 : 1),
          ),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: TextStyle(fontWeight: FontWeight.w600)),
            SizedBox(height: 4),
            Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
          ]),
        ),
      ),
    );
  }
}
