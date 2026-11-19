import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'auth/auth_store.dart';
import 'books/books_api.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';
import 'screens/name_screen.dart';
import 'settings/app_settings.dart';
import 'theme.dart';

void main() {
  final api = ApiClient();
  // Start waking the free server right away, while the user is still typing.
  api.wakeUp();
  final auth = AuthStore(api)..load();
  final settings = AppSettings()..load();
  runApp(BookDharaApp(auth: auth, books: BooksApi(api), settings: settings));
}

class BookDharaApp extends StatelessWidget {
  const BookDharaApp({super.key, required this.auth, required this.books, required this.settings});

  final AuthStore auth;
  final BooksApi books;
  final AppSettings settings;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: settings,
      builder: (context, _) => MaterialApp(
        title: 'BookDhara',
        debugShowCheckedModeBanner: false,
        theme: buildTheme(Brightness.light),
        darkTheme: buildTheme(Brightness.dark),
        themeMode: settings.themeMode,
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
            return HomeScreen(auth: auth, books: books, settings: settings);
          },
        ),
      ),
    );
  }
}
