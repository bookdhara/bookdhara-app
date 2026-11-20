// "Continue with Google" uses Google's own button, which only exists on the web for now.
// Other platforms (a future Android build) get an empty placeholder until native sign-in is added.
export 'google_button_stub.dart' if (dart.library.js_interop) 'google_button_web.dart';
