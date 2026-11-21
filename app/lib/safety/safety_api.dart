import '../api/api_client.dart';

enum ReportTarget { book, user }

/// Reasons a person can pick when reporting. [api] matches the server's names.
enum ReportReason {
  fakeListing('FAKE_LISTING', 'Fake or misleading listing', ReportTarget.book),
  wrongCondition('WRONG_CONDITION', 'Book condition is wrong', ReportTarget.book),
  spam('SPAM', 'Spam', null),
  inappropriate('INAPPROPRIATE', 'Inappropriate content', null),
  scam('SCAM', 'Scam or asking for money outside BookDhara', null),
  noShow('NO_SHOW', "Didn't show up to the meetup", ReportTarget.user),
  harassment('HARASSMENT', 'Rude or harassing', ReportTarget.user),
  other('OTHER', 'Something else', null);

  const ReportReason(this.api, this.label, this.onlyFor);
  final String api;
  final String label;
  final ReportTarget? onlyFor;

  static List<ReportReason> forTarget(ReportTarget t) =>
      values.where((r) => r.onlyFor == null || r.onlyFor == t).toList();
}

class BlockedUser {
  BlockedUser.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        name = j['name'] as String?;

  final int id;
  final String? name;
}

class SafetyApi {
  SafetyApi(this.api);

  final ApiClient api;

  Future<void> report(ReportTarget target, int id, ReportReason reason, String? details) async {
    await api.post('/api/reports', {
      'type': target == ReportTarget.book ? 'BOOK' : 'USER',
      'targetId': id,
      'reason': reason.api,
      if (details != null && details.trim().isNotEmpty) 'details': details.trim(),
    });
  }

  Future<void> block(int userId) async => api.put('/api/me/blocks/$userId');

  Future<void> unblock(int userId) async => api.delete('/api/me/blocks/$userId');

  Future<List<BlockedUser>> blocked() async {
    final items = await api.getList('/api/me/blocks');
    return items.map((e) => BlockedUser.fromJson(e as Map<String, dynamic>)).toList();
  }
}
