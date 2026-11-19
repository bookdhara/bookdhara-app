package com.bookdhara.wallet;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletController {

    private final WalletService wallet;

    public WalletController(WalletService wallet) {
        this.wallet = wallet;
    }

    @GetMapping("/api/wallet")
    public WalletService.Wallet myWallet(@AuthenticationPrincipal Long userId) {
        return wallet.walletOf(userId);
    }
}
