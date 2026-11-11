package com.whatsappconnect.database;
public final class AccountAccessDeniedException extends RuntimeException {
    public AccountAccessDeniedException() { super("Account access denied"); }
}
