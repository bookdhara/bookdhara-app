import 'package:bookdhara/auth/phone.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('accepts common ways of typing an Indian mobile number', () {
    for (final input in ['9876543210', '+919876543210', '09876543210', '98765 43210', '+91-98765-43210']) {
      expect(validateIndianMobile(input), isNull, reason: input);
    }
  });

  test('rejects empty and invalid numbers', () {
    expect(validateIndianMobile(''), 'Enter your mobile number');
    for (final input in ['12345', '5876543210', '98765432101', 'abcdefghij']) {
      expect(validateIndianMobile(input), isNotNull, reason: input);
    }
  });
}
