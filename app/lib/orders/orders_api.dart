import '../api/api_client.dart';
import 'order_models.dart';

class OrdersApi {
  OrdersApi(this.api);

  final ApiClient api;

  Future<Quote> quote(int bookId, {required bool pickup, int credits = 0}) async {
    final method = pickup ? 'PICKUP' : 'SHIPPING';
    return Quote.fromJson(await api.get('/api/orders/quote?bookId=$bookId&method=$method&credits=$credits'));
  }

  Future<OrderView> place(int bookId, {required bool pickup, required int credits, ShippingAddress? address}) async {
    return OrderView.fromJson(await api.post('/api/orders', {
      'bookId': bookId,
      'method': pickup ? 'PICKUP' : 'SHIPPING',
      'creditsToUse': credits,
      if (address != null) 'address': address.toJson(),
    }));
  }

  Future<List<OrderView>> mine({required bool selling}) async {
    final items = await api.getList('/api/orders?role=${selling ? 'selling' : 'buying'}');
    return items.map((e) => OrderView.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<OrderView> get(int id) async => OrderView.fromJson(await api.get('/api/orders/$id'));

  Future<OrderView> ship(int id, String? note) async =>
      OrderView.fromJson(await api.post('/api/orders/$id/ship', {'trackingNote': note}));

  Future<OrderView> confirmReceived(int id) async =>
      OrderView.fromJson(await api.post('/api/orders/$id/confirm-received', {}));

  Future<OrderView> handover(int id, String code) async =>
      OrderView.fromJson(await api.post('/api/orders/$id/handover', {'code': code}));

  Future<OrderView> cancel(int id, String? reason) async =>
      OrderView.fromJson(await api.post('/api/orders/$id/cancel', {'reason': reason}));

  Future<Wallet> wallet() async => Wallet.fromJson(await api.get('/api/wallet'));
}
