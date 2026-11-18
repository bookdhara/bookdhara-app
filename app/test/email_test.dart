import 'package:bookdhara/auth/email.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('accepts normal email addresses', () {
    for (final input in ['riya@gmail.com', ' Riya.S+books@Example.co.in ', 'a@b.io']) {
      expect(validateEmail(input), isNull, reason: input);
    }
  });

  test('rejects empty and malformed emails', () {
    expect(validateEmail(''), 'Enter your email');
    for (final input in ['riya', 'riya@gmail', '@gmail.com', 'riya@@gmail.com', 'ri ya@gmail.com', 'riya@gmail..com']) {
      expect(validateEmail(input), isNotNull, reason: input);
    }
  });
}
