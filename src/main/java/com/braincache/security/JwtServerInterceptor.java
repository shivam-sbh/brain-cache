package com.braincache.security;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

/**
 * gRPC server-side auth interceptor == a Go grpc.UnaryServerInterceptor that pulls
 * a token from metadata and stuffs the identity into the context.
 *
 * @GrpcGlobalServerInterceptor (net.devh) applies it to EVERY incoming call.
 * Policy here is permissive: if a valid token is present, bind the user's email into
 * the gRPC Context; if not, leave it anonymous and let the RPC decide (Register/Login
 * need no auth; guarded RPCs call GrpcSecurity.requireUserEmail() and get UNAUTHENTICATED).
 *
 * We deliberately do NOT reject unauthenticated calls here — that keeps public RPCs
 * open without maintaining a path allowlist. Enforcement lives at the method that needs it.
 */
@GrpcGlobalServerInterceptor
public class JwtServerInterceptor implements ServerInterceptor {

    private final JwtService jwt;

    public JwtServerInterceptor(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        Context ctx = Context.current();
        String authHeader = headers.get(GrpcSecurity.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(GrpcSecurity.BEARER_PREFIX)) {
            String token = authHeader.substring(GrpcSecurity.BEARER_PREFIX.length());
            try {
                String email = jwt.verify(token);   // throws if signature/expiry bad
                ctx = ctx.withValue(GrpcSecurity.USER_EMAIL, email);
            } catch (Exception ignored) {
                // Invalid token -> stay anonymous. requireUserEmail() will 401 downstream.
            }
        }

        // Run the rest of the call chain under our (possibly enriched) context.
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
