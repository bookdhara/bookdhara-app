/// Backend address. Override at build time with --dart-define=API_BASE=...
const String apiBase = String.fromEnvironment(
  'API_BASE',
  defaultValue: 'https://bookdhara-app.onrender.com',
);
