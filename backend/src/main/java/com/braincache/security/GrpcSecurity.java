package com.braincache.security;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * gRPC auth context holder + helpers. Same idea as stashing caller identity in
 * Go's context.Context via context.WithValue, then reading it back in a handler.
 *
 * Context.Key here == the typed key you'd use with context.WithValue.
 */
public final class GrpcSecurity {

    private GrpcSecurity() {
    }

    // Header clients send: "authorization: Bearer <jwt>".
    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    public static final String BEARER_PREFIX = "Bearer ";

    // Per-call slot holding the authenticated user's email (null if anonymous).
    public static final Context.Key<String> USER_EMAIL = Context.key("braincache.userEmail");

    /**
     * Read the current user's email inside a service method. Throws UNAUTHENTICATED
     * if the call carried no valid token. Use in RPCs that require auth.
     */
    public static String requireUserEmail() {
        String email = USER_EMAIL.get();
        if (email == null) {
            throw new StatusRuntimeException(
                    Status.UNAUTHENTICATED.withDescription("missing or invalid auth token"));
        }
        return email;
    }
}
