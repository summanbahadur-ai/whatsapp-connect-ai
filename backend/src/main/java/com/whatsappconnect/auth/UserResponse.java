package com.whatsappconnect.auth;

import java.time.Instant;
import java.util.UUID;
/** Safe public identity fields only. No credential, account permission or session secret. */
public record UserResponse(UUID id,String name,String email,boolean emailVerified,
                           String image,Instant createdAt,Instant updatedAt) {}
