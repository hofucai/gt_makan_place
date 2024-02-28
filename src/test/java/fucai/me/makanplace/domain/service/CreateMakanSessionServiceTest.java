package fucai.me.makanplace.domain.service;

import com.google.common.collect.ImmutableMap;
import fucai.me.makanplace.domain.enumeration.MakanSessionState;
import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanKaki;
import fucai.me.makanplace.domain.model.MakanSession;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;

/**
 * Test for R1: A user can initiate a session and invite others to join it.
 */
class CreateMakanSessionServiceTest {

    private MakanSessionRepository makanSessionRepository;
    private CreateMakanSessionService service;
    private UserEnrollmentChangeService changeService;


    public CreateMakanSessionServiceTest() {

    }

    public MakanSession.MakanSessionBuilder makanSessionTemplate() {
       return MakanSession.builder()
               .gatherTime(Instant.now().plus(Duration.ofHours(1)))
               .state(MakanSessionState.ACTIVE)
               .displayName("Test Lunch Session!");
    }

    @BeforeEach
    public void setup() {
        makanSessionRepository = Mockito.mock(MakanSessionRepository.class);
        changeService = new UserEnrollmentChangeService(makanSessionRepository);
        service = new CreateMakanSessionService(makanSessionRepository, changeService);
    }

    @Test
    public void testCreateFromCleanSlate() {
        /***
         * AC1 : A new session is created from scratch without any errors.
         *
         * Given: A user is not enrolled in another session
         * When: The user attempts to create a new session.
         *
         * The User provides:
         * Display Name of the Session
         * Display Name of the User
         * Date Time to meet
         * Then: A new makan session is created:
         *
         **/

        // Given user is not enrolled in any other session
        Mockito.when(makanSessionRepository.findByOwnerIdAndStateIsActive(Mockito.anyString())).thenReturn(null);
        Mockito.when(makanSessionRepository.findByMakanKakiIdAndStateIsActive(Mockito.anyString())).thenReturn(null);

        MakanKaki owner = MakanKaki.builder()
                .id("owner_id")
                .displayName("Owner Id")
                .build();
        MakanSession sessionToCreate = makanSessionTemplate()
                .owner(owner).build();

        service.createMakanSession(sessionToCreate);

        Mockito.verify(makanSessionRepository).save(sessionToCreate);
    }

    @Test
    @SneakyThrows
    public void testAbandonTheExistingOwnedSession() {
        /***
         AC2: The user is a session owner for an on-going session

         Given: A user owns an on-going session
         When: The user attempts to create a new session.

         The User provides:
         Display Name of the User
         Name of the session
         Date Time to meet
         Then: A new makan session is created:

         A shareable link is made available for other participants to enrol
         A link is provided for users to obtain current state of the session
         The on-going session is set to an ABANDONED state.
         *
         **/

        MakanKaki owner = MakanKaki.builder()
                .id("owner_id")
                .displayName("Owner Id")
                .build();

        // Given: A user owns an on-going session
        MakanSession existingOwnedSession = Mockito.spy(makanSessionTemplate().owner(owner).build());
        Mockito.when(makanSessionRepository.findByOwnerIdAndStateIsActive(Mockito.anyString()))
                .thenReturn(existingOwnedSession);
        Mockito.when(makanSessionRepository.findByMakanKakiIdAndStateIsActive(Mockito.anyString())).thenReturn(null);

        MakanSession sessionToCreate = makanSessionTemplate()
                .owner(owner).build();

        service.createMakanSession(sessionToCreate);

        Mockito.verify(makanSessionRepository).save(sessionToCreate);
        Mockito.verify(existingOwnedSession).abort();
        // On Going Session is set to abandoned state.
        Mockito.verify(makanSessionRepository).save(
                argThat(updatedExistingSession -> updatedExistingSession.equals(
                        existingOwnedSession.toBuilder().state(MakanSessionState.ABANDONED).build())));
    }

    @Test
    @SneakyThrows
    public void testWithdrawFromOtherSession() {
        /***
         * AC3: The user is a particiapnt in another active session
         Given: A user already participates in another active session
         When: The user attempts to create a new session.

         The User provides:
         Display Name of the User
         Name of the session
         Date Time to meet
         Then: A new makan session is created:

         A shareable link is made available for other participants to enrol
         A link is provided for users to obtain current state of the session
         What happens in the active session?
         The user is no longer listed as a participant of the other session.
         The choice submitted by the user is removed
         *
         **/

        MakanKaki owner = MakanKaki.builder()
                .id("owner_id")
                .displayName("Owner Id")
                .build();

        // Given: A user already participates in another active session
        MakanSession existingParticipatingSession = Mockito.spy(
                makanSessionTemplate()
                        .id("targetToBeUpdatedID")
                                .owner(MakanKaki.builder()
                                .displayName("some other person")
                                .id("other owner")
                                .build())
                        .participants(ImmutableMap.of(owner.getId(), owner))
                        .build());
        Mockito.when(makanSessionRepository.findByOwnerIdAndStateIsActive(Mockito.anyString()))
                .thenReturn(null);
        Mockito.when(makanSessionRepository.findByMakanKakiIdAndStateIsActive(Mockito.anyString()))
                .thenReturn(existingParticipatingSession);

        MakanSession sessionToCreate = makanSessionTemplate()
                .owner(owner).build();

        service.createMakanSession(sessionToCreate);

        Mockito.verify(makanSessionRepository).save(sessionToCreate);
        Mockito.verify(existingParticipatingSession).withdraw(owner.getId());
        // On Going Session is set to abandoned state.
        Mockito.verify(makanSessionRepository).save(
                argThat(updatedExistingSession ->
                        updatedExistingSession.getId().equals("targetToBeUpdatedID") &&
                        !updatedExistingSession.getParticipants().containsKey(owner.getId())));
    }


}