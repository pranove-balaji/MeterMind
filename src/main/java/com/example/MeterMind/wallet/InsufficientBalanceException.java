package com.example.MeterMind.wallet;

public class InsufficientBalanceException extends RuntimeException {

    private final long availableBalance;
    private final long requiredCredits;

    public InsufficientBalanceException(String message, long availableBalance, long requiredCredits) {
        super(message);
        this.availableBalance = availableBalance;
        this.requiredCredits = requiredCredits;
    }

    public long getAvailableBalance() {
        return availableBalance;
    }

    public long getRequiredCredits() {
        return requiredCredits;
    }
}
