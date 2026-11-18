import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'auth/auth_store.dart';
import 'books/books_api.dart';
import 'screens/home_screen.dart';
import 'screens/name_screen.dart';
import 'screens/login_screen.dart';
import 'theme.dart';

void main() {
  final api = ApiClient();
  final auth = AuthStore(api)..load();
  runApp(BookDharaApp(auth: auth, books: BooksApi(api)));
}

class BookDharaApp extends StatelessWidget {
  const BookDharaApp({super.key, required this.auth, required this.books});

  final AuthStore auth;
  final BooksApi books;

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
            return LoginScreen(auth: auth);
          }
          if (auth.user!.needsProfile) {
            return NameScreen(auth: auth);
          }
          return HomeScreen(auth: auth, books: books);
        },
      ),
    );
  }
}
