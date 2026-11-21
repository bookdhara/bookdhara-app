class ShippingAddress {
  ShippingAddress({required this.name, required this.phone, required this.address, required this.city, required this.pincode});

  factory ShippingAddress.fromJson(Map<String, dynamic> j) => ShippingAddress(
        name: j['name'] as String,
        phone: j['phone'] as String,
        address: j['address'] as String,
        city: j['city'] as String,
        pincode: j['pincode'] as String,
      );

  final String name;
  final String phone;
  final String address;
  final String city;
  final String pincode;

  Map<String, dynamic> toJson() => {'name': name, 'phone': phone, 'address': address, 'city': city, 'pincode': pincode};
}

class Quote {
  Quote.fromJson(Map<String, dynamic> j)
      : bookPrice = (j['bookPrice'] as num).toInt(),
        creditsUsed = (j['creditsUsed'] as num).toInt(),
        shippingFee = (j['shippingFee'] as num).toInt(),
        platformFee = (j['platformFee'] as num).toInt(),
        cashToPay = (j['cashToPay'] as num).toInt(),
        creditBalance = (j['creditBalance'] as num).toInt(),
        maxCredits = (j['maxCredits'] as num).toInt();

  final int bookPrice;
  final int creditsUsed;
  final int shippingFee;
  final int platformFee;
  final int cashToPay;
  final int creditBalance;
  final int maxCredits;
}

enum OrderStatus {
  placed('PLACED', 'Placed'),
  shipped('SHIPPED', 'On the way'),
  completed('COMPLETED', 'Completed'),
  cancelled('CANCELLED', 'Cancelled');

  const OrderStatus(this.api, this.label);

  final String api;
  final String label;

  static OrderStatus fromApi(String v) => OrderStatus.values.firstWhere((s) => s.api == v);
}

class OrderView {
  OrderView.fromJson(Map<String, dynamic> j)
      : id = (j['id'] as num).toInt(),
        status = OrderStatus.fromApi(j['status'] as String),
        isPickup = j['method'] == 'PICKUP',
        isBuyer = j['myRole'] == 'BUYER',
        bookId = ((j['book'] as Map<String, dynamic>)['id'] as num).toInt(),
        bookTitle = (j['book'] as Map<String, dynamic>)['title'] as String,
        bookAuthor = (j['book'] as Map<String, dynamic>)['author'] as String?,
        coverPhotoUrl = (j['book'] as Map<String, dynamic>)['coverPhotoUrl'] as String?,
        buyerName = j['buyerName'] as String?,
        sellerName = j['sellerName'] as String?,
        sellerCity = j['sellerCity'] as String?,
        bookPrice = (j['bookPrice'] as num).toInt(),
        creditsUsed = (j['creditsUsed'] as num).toInt(),
        shippingFee = (j['shippingFee'] as num).toInt(),
        platformFee = (j['platformFee'] as num).toInt(),
        cashPaid = (j['cashPaid'] as num).toInt(),
        testPayment = j['paymentMode'] == 'TEST',
        handoverCode = j['handoverCode'] as String?,
        sellerPayoutCredits = j['sellerPayout'] == 'CREDITS',
        sellerPayoutAmount = (j['sellerPayoutAmount'] as num?)?.toInt(),
        address = j['shippingAddress'] == null
            ? null
            : ShippingAddress.fromJson(j['shippingAddress'] as Map<String, dynamic>),
        trackingNote = j['trackingNote'] as String?,
        cancelReason = j['cancelReason'] as String?,
        canShip = (j['actions'] as Map<String, dynamic>)['canShip'] as bool,
        canConfirmReceived = (j['actions'] as Map<String, dynamic>)['canConfirmReceived'] as bool,
        canEnterHandoverCode = (j['actions'] as Map<String, dynamic>)['canEnterHandoverCode'] as bool,
        canCancel = (j['actions'] as Map<String, dynamic>)['canCancel'] as bool,
        createdAt = DateTime.parse(j['createdAt'] as String).toLocal();

  final int id;
  final OrderStatus status;
  final bool isPickup;
  final bool isBuyer;
  final int bookId;
  final String bookTitle;
  final String? bookAuthor;
  final String? coverPhotoUrl;
  final String? buyerName;
  final String? sellerName;
  final String? sellerCity;
  final int bookPrice;
  final int creditsUsed;
  final int shippingFee;
  final int platformFee;
  final int cashPaid;
  final bool testPayment;
  final String? handoverCode;
  final bool sellerPayoutCredits;
  final int? sellerPayoutAmount;
  final ShippingAddress? address;
  final String? trackingNote;
  final String? cancelReason;
  final bool canShip;
  final bool canConfirmReceived;
  final bool canEnterHandoverCode;
  final bool canCancel;
  final DateTime createdAt;

  /// One line for alerts: what I need to do next on this order.
  String get nextStep {
    if (isBuyer) return '"$bookTitle" is on its way. Confirm when it arrives.';
    if (isPickup) return '${buyerName ?? 'Someone'} bought "$bookTitle". Meet up and enter their 4-digit code.';
    return '${buyerName ?? 'Someone'} bought "$bookTitle". Ship it and tap "Mark as shipped".';
  }

  String get otherPartyName => (isBuyer ? sellerName : buyerName) ?? 'a reader';
}

class WalletEntry {
  WalletEntry.fromJson(Map<String, dynamic> j)
      : amount = (j['amount'] as num).toInt(),
        kind = j['kind'] as String,
        note = j['note'] as String,
        createdAt = DateTime.parse(j['createdAt'] as String).toLocal();

  final int amount;
  final String kind;
  final String note;
  final DateTime createdAt;
}

class Wallet {
  Wallet.fromJson(Map<String, dynamic> j)
      : credits = (j['credits'] as num).toInt(),
        creditsOnHold = (j['creditsOnHold'] as num).toInt(),
        cashEarned = (j['cashEarned'] as num).toInt(),
        history = (j['history'] as List<dynamic>).map((e) => WalletEntry.fromJson(e as Map<String, dynamic>)).toList();

  final int credits;
  final int creditsOnHold;
  final int cashEarned;
  final List<WalletEntry> history;
}

/// "18 Nov" style date for lists.
String shortDate(DateTime d) {
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return '${d.day} ${months[d.month - 1]}';
}
