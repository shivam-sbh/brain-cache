package com.braincache.grpc;

import com.braincache.grpc.proto.Card;
import com.braincache.grpc.proto.CardServiceGrpc;
import com.braincache.grpc.proto.CreateCardRequest;
import com.braincache.grpc.proto.DsaProblem;
import com.braincache.grpc.proto.ListDueCardsRequest;
import com.braincache.grpc.proto.ListDueCardsResponse;
import com.braincache.grpc.proto.ListHistoryRequest;
import com.braincache.grpc.proto.ListHistoryResponse;
import com.braincache.grpc.proto.MarkDsaProblemDoneRequest;
import com.braincache.grpc.proto.ReviewCardRequest;
import com.braincache.grpc.proto.SearchDsaProblemsRequest;
import com.braincache.grpc.proto.SearchDsaProblemsResponse;
import com.braincache.security.GrpcSecurity;
import com.braincache.service.CardService;
import com.braincache.service.DsaCatalog;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * Transport adapter for CardService. Same shape as AuthGrpcService: proto <-> domain,
 * delegate, map errors. Every RPC is guarded — identity comes from the JWT the
 * interceptor verified, read via GrpcSecurity.requireUserEmail(). No token -> UNAUTHENTICATED.
 *
 * Naming clash note: the proto message is com.braincache.grpc.proto.Card; the JPA
 * entity is com.braincache.domain.Card. This class only imports the proto one and
 * maps from the domain one by fully-qualified name in toProto().
 */
@GrpcService
public class CardGrpcService extends CardServiceGrpc.CardServiceImplBase {

    private final CardService cardService;
    private final DsaCatalog catalog;

    public CardGrpcService(CardService cardService, DsaCatalog catalog) {
        this.cardService = cardService;
        this.catalog = catalog;
    }

    @Override
    public void createCard(CreateCardRequest request, StreamObserver<Card> observer) {
        guarded(observer, email ->
                toProto(cardService.create(email, request.getFront(), request.getBack())));
    }

    @Override
    public void listDueCards(ListDueCardsRequest request, StreamObserver<ListDueCardsResponse> observer) {
        try {
            String email = GrpcSecurity.requireUserEmail();
            ListDueCardsResponse.Builder reply = ListDueCardsResponse.newBuilder();
            cardService.listDue(email).forEach(c -> reply.addCards(toProto(c)));
            observer.onNext(reply.build());
            observer.onCompleted();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    @Override
    public void reviewCard(ReviewCardRequest request, StreamObserver<Card> observer) {
        guarded(observer, email ->
                toProto(cardService.review(email, request.getCardId(), request.getPassed())));
    }

    @Override
    public void searchDsaProblems(SearchDsaProblemsRequest request, StreamObserver<SearchDsaProblemsResponse> observer) {
        try {
            GrpcSecurity.requireUserEmail(); // guarded, though it reads only static catalog
            SearchDsaProblemsResponse.Builder reply = SearchDsaProblemsResponse.newBuilder();
            for (DsaCatalog.Problem p : catalog.search(request.getQuery(), request.getLimit())) {
                reply.addProblems(DsaProblem.newBuilder()
                        .setTitle(p.title())
                        .setUrl(p.url())
                        .setNumCompanies(p.numCompanies())
                        .addAllCompanies(p.companyList())
                        .build());
            }
            observer.onNext(reply.build());
            observer.onCompleted();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    @Override
    public void markDsaProblemDone(MarkDsaProblemDoneRequest request, StreamObserver<Card> observer) {
        guarded(observer, email ->
                toProto(cardService.markDsaDone(email, request.getUrl(), request.getComment())));
    }

    @Override
    public void listHistory(ListHistoryRequest request, StreamObserver<ListHistoryResponse> observer) {
        try {
            String email = GrpcSecurity.requireUserEmail();
            ListHistoryResponse.Builder reply = ListHistoryResponse.newBuilder();
            cardService.listHistory(email).forEach(c -> reply.addCards(toProto(c)));
            observer.onNext(reply.build());
            observer.onCompleted();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    // Shared plumbing for the single-Card responses: resolve identity, run the action,
    // emit, or convert failures to gRPC Status. Mirrors AuthGrpcService.handle().
    private void guarded(StreamObserver<Card> observer, java.util.function.Function<String, Card> action) {
        try {
            String email = GrpcSecurity.requireUserEmail();
            observer.onNext(action.apply(email));
            observer.onCompleted();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    private Card toProto(com.braincache.domain.Card c) {
        // proto strings can't be null; coalesce the DSA-only fields to "".
        String url = c.getUrl() == null ? "" : c.getUrl();
        Card.Builder b = Card.newBuilder()
                .setId(c.getId())
                .setFront(c.getFront())
                .setBack(c.getBack())
                .setIntervalIndex(c.getIntervalIndex())
                .setNextReview(c.getNextReview().toString())
                .setType(c.getType().name())
                .setUrl(url)
                .setComment(c.getComment() == null ? "" : c.getComment())
                .setCreatedAt(c.getCreatedAt().toString());
        // Enrich DSA cards with the company list from the catalog (by URL).
        if (!url.isEmpty()) {
            catalog.byUrl(url).ifPresent(p -> b
                    .setNumCompanies(p.numCompanies())
                    .addAllCompanies(p.companyList()));
        }
        return b.build();
    }
}
