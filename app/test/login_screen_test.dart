import 'package:bookdhara/api/api_client.dart';
import 'package:bookdhara/auth/auth_store.dart';
import 'package:bookdhara/screens/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows an error for an invalid email without calling the server', (tester) async {
    final auth = AuthStore(ApiClient(baseUrl: 'http://localhost:1'));
    await tester.pumpWidget(MaterialApp(home: LoginScreen(auth: auth)));

    expect(find.text('Log in with your email'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'riya@gmail');
    await tester.tap(find.text('Send code'));
    await tester.pump();

    expect(find.text('Enter a valid email address'), findsOneWidget);
  });
}
