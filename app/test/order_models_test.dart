import 'package:bookdhara/orders/order_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('order view reads the API shape and the actions', () {
    final o = OrderView.fromJson({
      'id': 12,
      'status': 'PLACED',
      'method': 'PICKUP',
      'myRole': 'BUYER',
      'book': {'id': 3, 'title': 'Atomic Habits', 'author': 'James Clear', 'coverPhotoUrl': null},
      'buyerName': 'Aman',
      'sellerName': 'Riya',
      'sellerCity': 'Mumbai',
      'bookPrice': 160,
      'creditsUsed': 0,
      'shippingFee': 0,
      'platformFee': 5,
      'cashPaid': 165,
      'paymentMode': 'TEST',
      'handoverCode': '4729',
      'sellerPayout': null,
      'sellerPayoutAmount': null,
      'shippingAddress': null,
      'trackingNote': null,
      'cancelReason': null,
      'actions': {'canShip': false, 'canConfirmReceived': false, 'canEnterHandoverCode': false, 'canCancel': true},
      'createdAt': '2026-11-19T10:00:00Z',
      'completedAt': null,
    });
    expect(o.isPickup, isTrue);
    expect(o.isBuyer, isTrue);
    expect(o.otherPartyName, 'Riya');
    expect(o.handoverCode, '4729');
    expect(o.canCancel, isTrue);
    expect(o.status, OrderStatus.placed);
  });

  test('short dates', () {
    expect(shortDate(DateTime(2026, 11, 19)), '19 Nov');
  });
}
