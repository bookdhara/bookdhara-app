/// Returns an error message for an invalid Indian mobile number, or null if it looks fine.
/// The backend does the final check; this just catches typos early.
String? validateIndianMobile(String input) {
  var digits = input.replaceAll(RegExp(r'[\s\-()]'), '');
  if (digits.startsWith('+91')) {
    digits = digits.substring(3);
  } else if (digits.startsWith('0') && digits.length == 11) {
    digits = digits.substring(1);
  }
  if (digits.isEmpty) {
    return 'Enter your mobile number';
  }
  if (!RegExp(r'^[6-9]\d{9}$').hasMatch(digits)) {
    return 'Enter a valid 10-digit mobile number';
  }
  return null;
}
