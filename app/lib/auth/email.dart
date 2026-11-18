/// Returns an error message for an email that is clearly wrong, or null if it looks fine.
/// The backend does the final check; this just catches typos early.
String? validateEmail(String input) {
  final email = input.trim();
  if (email.isEmpty) {
    return 'Enter your email';
  }
  if (!RegExp(r'^[^@\s]+@[^@\s]+\.[A-Za-z]{2,}$').hasMatch(email) || email.contains('..')) {
    return 'Enter a valid email address';
  }
  return null;
}
