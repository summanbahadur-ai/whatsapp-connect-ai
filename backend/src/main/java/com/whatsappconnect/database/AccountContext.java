package com.whatsappconnect.database;
import java.util.UUID;
public record AccountContext(UUID userId, UUID accountId, AccountRole role) {}
