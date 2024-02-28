package fucai.me.makanplace.domain.model;

import com.google.common.collect.ImmutableMap;
import fucai.me.makanplace.domain.enumeration.MakanSessionState;
import fucai.me.makanplace.domain.exception.business.DuplicatedDisplayNameInSessionException;
import fucai.me.makanplace.domain.exception.business.KakiNotEnrolledException;
import fucai.me.makanplace.domain.exception.business.MakanSesssionClosedException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MakanSessionTest {

    private static final String OWNER_ID = "ownerKakiUID";

    public MakanSession.MakanSessionBuilder templateBuilder() {
        return
        MakanSession.builder()
                .displayName("Lunch Time with Test Buddy")
                .state(MakanSessionState.ACTIVE)
                .gatherTime(Instant.now().plus(Duration.ofHours(1)))
                .owner(MakanKaki.builder()
                        .displayName("owner kaki")
                        .id(OWNER_ID)
                        .build());
    }

    @Test
    @SneakyThrows
    void testAbortForActiveSession() {
        final MakanSession activeSession = templateBuilder().build();
        final MakanSession abortedSession = activeSession.abort();
        assertEquals(MakanSessionState.ABANDONED, abortedSession.getState());
    }

    @Test
    void testAbortForDecidedSession() {
        final MakanSession decidedSession = templateBuilder()
                .state(MakanSessionState.DECIDED).build();
        assertThrows(MakanSesssionClosedException.class, () -> decidedSession.abort());
    }

    @Test
    void testIsOwnerMatches() {
        assertTrue(templateBuilder().build().isOwner(OWNER_ID));
    }

    @Test
    void testIsOwnerNotMatches() {
        assertFalse(templateBuilder().build().isOwner(OWNER_ID + "MISMATCH"));
    }

    @Test
    void testSessionCannotBeActiveAndHaveDecidedPlace() {
        assertThrows(IllegalStateException.class, () ->
            templateBuilder().selectedPlace(
                        Optional.of(
                                MakanPlace.builder()
                                                .placeName("Some fake place")
                                        .build()))
                    .state(MakanSessionState.ACTIVE)
                    .build()
        );
    }

    @Test
    @SneakyThrows
    void suggestPlaceFreshSuggestion() {
        // Given: Participant has enrolled in the session
        // When: Participant suggests a place  for the session
        // Then: The suggested place will be logged
        final MakanKaki kaki = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("John Wick")
                .build();

        MakanSession session = templateBuilder().build()
                .enroll(kaki)
                .suggestPlace(kaki.getId(), "McDonalds");

        assertEquals(session.getParticipants().size(), 1);
        assertEquals(session.getParticipants().get(kaki.getId()), kaki);

        assertTrue(session.getSelectedPlace().isEmpty());
        final MakanPlace expectedMakanPlace =
                MakanPlace.builder().suggesters(ImmutableMap.of(kaki.getId(), kaki)).placeName("McDonalds").build();
        assertEquals(1, session.getSuggestedPlaces().size());
        assertEquals(expectedMakanPlace, session.getSuggestedPlaces().get("McDonalds"));
    }
    @Test
    @SneakyThrows
    void suggestPlaceWithoutEnrollment() {
        // Given: Participant has not enrolled in the session
        // When: Participant suggests a place  for the session
        // Then: An exception will be thrown
        final MakanKaki kaki = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("John Wick")
                .build();

        assertThrows(KakiNotEnrolledException.class, () -> templateBuilder().build()
                .suggestPlace(kaki.getId(), "McDonalds"));
    }


    @Test
    void suggestPlaceOnAbandonedSession() {
        final MakanKaki kaki = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("John Wick")
                .build();

        assertThrows(MakanSesssionClosedException.class, ()->
            templateBuilder()
                .state(MakanSessionState.ABANDONED)
                .build()
                .suggestPlace(kaki.getId(), "McDonalds"));
    }

    @Test
    void suggestPlaceOnADecidedSession() {
        final MakanKaki kaki = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("John Wick")
                .build();

        assertThrows(MakanSesssionClosedException.class, ()->
                templateBuilder()
                        .state(MakanSessionState.DECIDED)
                        .build()
                        .suggestPlace(kaki.getId(), "McDonalds"));
    }

    @Test
    @SneakyThrows
    void suggestPlaceDuplicateSuggestion() {

        // Given: A participant has already suggested "KFC"
        // When: The second participant suggests "KFC"
        // Then: KFC should have 2 makan kakis as suggestors
        final MakanKaki kaki1 = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("John Wick")
                .build();

        final MakanKaki kaki2 = MakanKaki.builder()
                .id("kaki_id2")
                .displayName("Mr. Anderson")
                .build();

        MakanSession session = templateBuilder().build()
                .enroll(kaki1)
                .enroll(kaki2)
                .suggestPlace(kaki1.getId(), "KFC")
                .suggestPlace(kaki2.getId(), "KFC");


        // When a participant hasn't enrolled and suggests a place, the participant will automagically be enrolled.
        assertEquals(session.getParticipants().size(), 2);
        assertEquals(session.getParticipants().get(kaki1.getId()), kaki1);
        assertEquals(session.getParticipants().get(kaki2.getId()), kaki2);

        assertTrue(session.getSelectedPlace().isEmpty());

        final MakanPlace expectedMakanPlace =
                MakanPlace.builder()
                        .suggesters(ImmutableMap.of(kaki1.getId(), kaki1, kaki2.getId(), kaki2))
                        .placeName("KFC").build();
        assertEquals(1, session.getSuggestedPlaces().size());
        assertEquals(expectedMakanPlace, session.getSuggestedPlaces().get("KFC"));
    }

    @Test
    @SneakyThrows
    void decideBasedOnRandomSelectionPolicy() {
        final MakanKaki kaki1 = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("Freddie Mercury")
                .build();

        MakanSession session = templateBuilder().build()
                .enroll(kaki1)
                .suggestPlace(kaki1.getId(), "KFC")
                .suggestPlace(kaki1.getId(), "KopiTiam")
                .suggestPlace(kaki1.getId(), "McDonalds")
                .suggestPlace(kaki1.getId(), "Mos Burger")
                .suggestPlace(kaki1.getId(), "Amoy Street Hawker")
                .suggestPlace(kaki1.getId(), "Wee Nam Kee Chicken Rice")
                .suggestPlace(kaki1.getId(), "Bismillah Briyani")
                .suggestPlace(kaki1.getId(), "Tabao Donki");

        final RandomSelectionDecisionPolicy randomSelectionDecisionPolicy =
                // setting a fixed seed makes the "random" predictable; we will always get "KFC"
                new RandomSelectionDecisionPolicy(new Random(0L));

        assertTrue(session.getSelectedPlace().isEmpty());

        final MakanSession decidedSession = session.decide(randomSelectionDecisionPolicy);
        final MakanPlace expectedPlace = MakanPlace.builder()
                .placeName("KFC")
                .suggesters(ImmutableMap.of(kaki1.getId(), kaki1))
                .build();
        assertEquals(expectedPlace, decidedSession.getSelectedPlace().get());
        assertEquals(MakanSessionState.DECIDED, decidedSession.getState());
    }

    @Test
    @SneakyThrows
    void decideOnClosedSession() {
        final MakanKaki kaki1 = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("Freddie Mercury")
                .build();

        MakanSession session = templateBuilder().build()
                .enroll(kaki1)
                .suggestPlace(kaki1.getId(), "KFC")
                .suggestPlace(kaki1.getId(), "KopiTiam")
                .suggestPlace(kaki1.getId(), "McDonalds")
                .suggestPlace(kaki1.getId(), "Mos Burger")
                .suggestPlace(kaki1.getId(), "Amoy Street Hawker")
                .suggestPlace(kaki1.getId(), "Wee Nam Kee Chicken Rice")
                .suggestPlace(kaki1.getId(), "Bismillah Briyani")
                .suggestPlace(kaki1.getId(), "Tabao Donki")
                // state is set to abandoned.
                .abort();


        final RandomSelectionDecisionPolicy randomSelectionDecisionPolicy =
                // setting a fixed seed makes the "random" predictable; we will always get "KFC"
                new RandomSelectionDecisionPolicy(new Random(0L));

        assertThrows(MakanSesssionClosedException.class, () -> session.decide(randomSelectionDecisionPolicy));
    }

    @Test
    @SneakyThrows
    void enroll() {
        MakanSession session = templateBuilder().build();
        final MakanKaki kaki = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("John Wick")
                .build();

        assertTrue(session.getParticipants().isEmpty());
        session = session.enroll(kaki);

        assertEquals(1, session.getParticipants().size());
        assertEquals(kaki, session.getParticipants().values().stream().findAny().get());
    }
    @Test
    @SneakyThrows
    void enrollDisplayNameClashesWithOwner() {
        MakanSession session = templateBuilder().build();
        final MakanKaki kaki = MakanKaki.builder()
                .id("kaki_id1")
                .displayName(session.getOwner().getDisplayName())
                .build();

        assertTrue(session.getParticipants().isEmpty());
        assertThrows(DuplicatedDisplayNameInSessionException.class, () -> session.enroll(kaki));
    }

    @Test
    @SneakyThrows
    void enrollDisplayNameClashesWithOtherParticipants() {
        MakanSession session = templateBuilder().build();
        final MakanKaki kaki1 = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("foo")
                .build();
        session = session.enroll(kaki1);
        final MakanKaki kaki2 = MakanKaki.builder()
                .id("kaki_id2")
                .displayName("foo")
                .build();

        MakanSession finalSession = session;
        assertThrows(DuplicatedDisplayNameInSessionException.class, () -> finalSession.enroll(kaki2));
    }


    @Test
    @SneakyThrows
    void enrollOnClosedSession() {
        MakanSession session = templateBuilder()
                .build()
                .abort();
        final MakanKaki kaki = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("John Wick")
                .build();

        assertTrue(session.getParticipants().isEmpty());
        assertThrows(MakanSesssionClosedException.class, () -> session.enroll(kaki));
    }


    @Test
    @SneakyThrows
    void withdrawPlaceHasNoOtherParticipants() {
        // GIVEN: Participant is the only person who suggested "Burger King"
        // When: Participant withdraws from the session.
        // Then: "Burger King" is no longer listed as a suggested place

        final MakanKaki kaki1 = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("Freddie Mercury")
                .build();

        MakanSession session = templateBuilder().build()
                .enroll(kaki1)
                .suggestPlace(kaki1.getId(), "Burger King");
        assertTrue(session.getSuggestedPlaces().containsKey("Burger King"));

        session = session.withdraw(kaki1.getId());
        assertFalse(session.getParticipants().containsValue(kaki1), "kaki 1 no longer is a participant");
        assertFalse(session.getSuggestedPlaces().containsKey("Burger King"));
    }

    @Test
    @SneakyThrows
    void withdrawPlaceHasOtherParticipants() {
        // GIVEN: 2 Participants suggested  "Burger King"
        // When: 1 Participant withdraws from the session.
        // Then: "Burger King" is still listed as a suggested place

        final MakanKaki kaki1 = MakanKaki.builder()
                .id("kaki_id1")
                .displayName("Freddie Mercury")
                .build();

        final MakanKaki kaki2 = MakanKaki.builder()
                .id("kaki_id2")
                .displayName("Bon Jovi")
                .build();

        MakanSession session = templateBuilder().build()
                .enroll(kaki1)
                .enroll(kaki2)
                .suggestPlace(kaki1.getId(), "Burger King")
                .suggestPlace(kaki2.getId(), "Burger King");
        assertTrue(session.getSuggestedPlaces().containsKey("Burger King"));

        session = session.withdraw(kaki1.getId());
        assertTrue(session.getSuggestedPlaces().containsKey("Burger King"),
                "Burger King is still a suggested place");
        assertTrue(session.getSuggestedPlaces().get("Burger King").getSuggesters().containsValue(kaki2),
                "Kaki 2 should still be listed as a suggestor for burger king");
    }


}