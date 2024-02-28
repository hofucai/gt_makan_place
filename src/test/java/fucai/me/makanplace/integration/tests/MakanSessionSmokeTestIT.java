package fucai.me.makanplace.integration.tests;

import com.google.common.collect.ImmutableSet;
import fucai.me.makanplace.MakanPlaceApplication;
import fucai.me.makanplace.api.dto.*;
import fucai.me.makanplace.domain.enumeration.MakanSessionState;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MakanPlaceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class MakanSessionSmokeTestIT {
    private final String BASE_URL = "/makan-session";

    private class MakanSessionRepresentationModel extends RepresentationModel<MakanSessionResource> {}

    public HttpHeaders createSessionCookieHeader(ResponseEntity responseEntity) {
        final HttpHeaders returnHeaders  = new HttpHeaders();
        returnHeaders.add(HttpHeaders.COOKIE, responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE).getFirst());
        return returnHeaders;
    }
    @Test
    void exampleTest(@Autowired TestRestTemplate restTemplate) {

        final Set<String> suggestedPlaces = new HashSet<>();
        final String makanSessionId;

        CreateMakanSessionRequest createMakanSessionRequest = CreateMakanSessionRequest.builder()
                .makanSessionDisplayName("Smoke Test Session")
                .gatherTime(Instant.now().plus(Duration.ofHours(2)))
                .userDisplayName("Session Owner")
                .build();

        // Owner of the session creates the session
        HttpEntity<CreateMakanSessionRequest> createSesionEntity = new HttpEntity(createMakanSessionRequest);
        ResponseEntity<MakanSessionResource> createSessionResponseEntity =
                restTemplate.exchange(BASE_URL, HttpMethod.POST, createSesionEntity,  MakanSessionResource.class);

        final HttpHeaders ownerCookieHeader  = createSessionCookieHeader(createSessionResponseEntity);
        final MakanSessionResource createdSessionResponse = createSessionResponseEntity.getBody();

        assertEquals(HttpStatus.CREATED.value(), createSessionResponseEntity.getStatusCode().value());
        assertEquals(MakanSessionState.ACTIVE.name(),createdSessionResponse.getState().name());
        assertEquals(createMakanSessionRequest.getMakanSessionDisplayName(),
                createMakanSessionRequest.getMakanSessionDisplayName());
        assertEquals(createMakanSessionRequest.getUserDisplayName(),
                createMakanSessionRequest.getUserDisplayName());
        assertEquals(createMakanSessionRequest.getUserDisplayName(), createdSessionResponse.getOwnerName());
        makanSessionId = createdSessionResponse.getMakanSessionId();;

        // Given: Owner has created a session
        // When:  Owmer suggests KFC
        // Then:  KFC is added to the list of makan places with owner listed as suggester.

        final SuggestPlaceRequest ownerSuggestsKFCRequest = SuggestPlaceRequest.builder()
                .placeName("KFC")
                .build();
        suggestedPlaces.add(ownerSuggestsKFCRequest.getPlaceName());


        HttpEntity<CreateMakanSessionRequest> ownerSuggestKFCEntity =
                new HttpEntity(ownerSuggestsKFCRequest, ownerCookieHeader);
        ResponseEntity<MakanSessionResource> ownerSuggestKFCResponse =
                restTemplate.exchange(BASE_URL + "/" + makanSessionId + "/suggest-place",
                        HttpMethod.POST, ownerSuggestKFCEntity,  MakanSessionResource.class);

        final MakanSessionResource ownerCreatesKFCSuggestionResponse = ownerSuggestKFCResponse.getBody();

        assertEquals(
                ImmutableSet.of(SuggestedMakanPlace.builder()
                        .makanPlace("KFC").suggester(ImmutableSet.of("Session Owner")).build()),
                ownerCreatesKFCSuggestionResponse.getSuggestedMakanPlaces());

        // Owner makes an additional suggestion of burger king
        final SuggestPlaceRequest ownerSuggestsBurgerKingRequest = SuggestPlaceRequest.builder()
                .placeName("Burger King")
                .build();
        suggestedPlaces.add(ownerSuggestsBurgerKingRequest.getPlaceName());


        HttpEntity<CreateMakanSessionRequest> ownerSuggestBurgerKingEntity =
                new HttpEntity(ownerSuggestsBurgerKingRequest, ownerCookieHeader);
        restTemplate.exchange(BASE_URL + "/" + makanSessionId + "/suggest-place",
                        HttpMethod.POST, ownerSuggestBurgerKingEntity,  MakanSessionResource.class);

        // Given: There exists a makan session
        // When: User wansts to participate in the session and enrols in it
        // Then: User is reflected as a participant

        HttpEntity<CreateMakanSessionRequest> enrollSessionEntity = new HttpEntity(
                EnrollMakanSessionRequest.builder()
                        .userDisplayName("p1")
                        .build()
        );
        ResponseEntity<MakanSessionResource> enrollParticipantResponseEntity =
                restTemplate.exchange(BASE_URL + "/" + makanSessionId + "/enroll",
                        HttpMethod.POST, enrollSessionEntity,  MakanSessionResource.class);

        final HttpHeaders participantCookie  = createSessionCookieHeader(enrollParticipantResponseEntity);
        final MakanSessionResource enrollSessionResponse = enrollParticipantResponseEntity.getBody();

        assertEquals(ImmutableSet.of("p1"), enrollSessionResponse.getParticipants());

        // Given: Participant is enrolled in the session
        // When:  Participant suggests MOS Burger
        // Then:  MOS Burger is added to the list of makan places with participant listed as suggester.

        final SuggestPlaceRequest participantSuggestsMOSRequest = SuggestPlaceRequest.builder()
                .placeName("MOS Burger")
                .build();
        suggestedPlaces.add(participantSuggestsMOSRequest.getPlaceName());


        HttpEntity<CreateMakanSessionRequest> participantSuggestMOSEntity =
                new HttpEntity(participantSuggestsMOSRequest, participantCookie);
        ResponseEntity<MakanSessionResource> participantSuggestMOSResponseEntity =
                restTemplate.exchange(BASE_URL + "/" + makanSessionId + "/suggest-place",
                        HttpMethod.POST, participantSuggestMOSEntity,  MakanSessionResource.class);

        MakanSessionResource participantSuggestMosBurgerResponse = participantSuggestMOSResponseEntity.getBody();
        assertEquals(ImmutableSet.of(
                SuggestedMakanPlace.builder()
                        .makanPlace("KFC")
                        .suggester(ImmutableSet.of("Session Owner"))
                        .build(),
                SuggestedMakanPlace.builder()
                    .makanPlace("Burger King")
                    .suggester(ImmutableSet.of("Session Owner"))
                    .build(),
                SuggestedMakanPlace.builder()
                    .makanPlace("MOS Burger")
                    .suggester(ImmutableSet.of("p1"))
                    .build()
                ), participantSuggestMosBurgerResponse.getSuggestedMakanPlaces());

        // Given: There is an on-going session with a list of suggested places
        // When: Owner closes the session
        // Then: The makan place is selected and the session state is updated to DECIDED
        HttpEntity decideSessionEntity =
                new HttpEntity(null, ownerCookieHeader);
        ResponseEntity<MakanSessionResource> closeSessionResponseEntity =
                restTemplate.exchange(BASE_URL + "/" + makanSessionId + "/decide",
                        HttpMethod.POST, decideSessionEntity,  MakanSessionResource.class);

        MakanSessionResource closedSession = closeSessionResponseEntity.getBody();
        assertTrue(suggestedPlaces.contains(closedSession.getSelectedMakanPlace().getMakanPlace()));
        assertEquals(MakanSessionState.DECIDED.name(), closedSession.getState().name());

    }

}
