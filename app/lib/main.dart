import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'auth/auth_store.dart';
import 'screens/home_screen.dart';
import 'screens/name_screen.dart';
import 'screens/phone_screen.dart';
import 'theme.dart';

void main() {
  final auth = AuthStore(ApiClient())..load();
  runApp(BookDharaApp(auth: auth));
}

class BookDharaApp extends StatelessWidget {
  const BookDharaApp({super.key, required this.auth});

  final AuthStore auth;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BookDhara',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(),
      home: ListenableBuilder(
        listenable: auth,
        builder: (context, _) {
          if (!auth.loaded) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }
          if (!auth.isLoggedIn) {
            return PhoneScreen(auth: auth);
          }
          if (auth.user!.needsProfile) {
            return NameScreen(auth: auth);
          }
          return HomeScreen(auth: auth);
        },
      ),
    );
  }
}
