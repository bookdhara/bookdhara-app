import 'package:bookdhara/api/api_client.dart';
import 'package:bookdhara/auth/auth_store.dart';
import 'package:bookdhara/screens/phone_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows an error for an invalid number without calling the server', (tester) async {
    final auth = AuthStore(ApiClient(baseUrl: 'http://localhost:1'));
    await tester.pumpWidget(MaterialApp(home: PhoneScreen(auth: auth)));

    expect(find.text('Log in with your phone'), findsOneWidget);

    await tester.enterText(find.byType(TextField), '12345');
    await tester.tap(find.text('Send OTP'));
    await tester.pump();

    expect(find.text('Enter a valid 10-digit mobile number'), findsOneWidget);
  });
}
