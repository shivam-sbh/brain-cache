package com.braincache.grpc;

import com.braincache.grpc.proto.AuthResponse;
import com.braincache.grpc.proto.AuthServiceGrpc;
import com.braincache.grpc.proto.LoginRequest;
import com.braincache.grpc.proto.RegisterRequest;
import com.braincache.service.AuthService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * Transport adapter == your pb.UnimplementedAuthServiceServer impl.
 * @GrpcService (net.devh) registers this bean with the gRPC server started on :9090.
 * Job: translate proto <-> domain, delegate to AuthService, map errors to gRPC Status.
 * No business logic here — thin, like a good handler.
 */
@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    public AuthGrpcService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        handle(responseObserver, () ->
                authService.register(request.getEmail(), request.getPassword()));
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        handle(responseObserver, () ->
                authService.login(request.getEmail(), request.getPassword()));
    }

    // Shared plumbing: run the call, emit response, or convert failure to a gRPC Status.
    // StreamObserver is the unary-response sink (onNext once, then onCompleted).
    private void handle(StreamObserver<AuthResponse> observer,
                        java.util.function.Supplier<AuthService.AuthResult> action) {
        try {
            AuthService.AuthResult result = action.get();
            AuthResponse reply = AuthResponse.newBuilder()
                    .setToken(result.token())
                    .setEmail(result.email())
                    .build();
            observer.onNext(reply);
            observer.onCompleted();
        } catch (IllegalArgumentException e) {
            // Bad input / auth failure -> INVALID_ARGUMENT with the message.
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}
