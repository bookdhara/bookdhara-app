{{flutter_js}}
{{flutter_build_config}}

// Load the app without a service worker. A service worker kept serving an old copy of the app
// after updates, so people didn't see new features (like the Google button) until they cleared data.
_flutter.loader.load();
