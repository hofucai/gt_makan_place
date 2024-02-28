package fucai.me.makanplace.app.service;

import fucai.me.makanplace.domain.enumeration.MakanSessionState;
import fucai.me.makanplace.domain.exception.ForbiddenOperationException;
import fucai.me.makanplace.domain.exception.MakanSessionIDNotFoundException;
import fucai.me.makanplace.domain.exception.business.BusinessRuleException;
import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanKaki;
import fucai.me.makanplace.domain.model.MakanSession;
import fucai.me.makanplace.domain.service.CreateMakanSessionService;
import fucai.me.makanplace.domain.service.DecideMakanPlaceService;
import fucai.me.makanplace.domain.service.EnrolMakanSessionService;
import fucai.me.makanplace.domain.service.SuggestMakanPlaceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Application Layer Service that converts inputs into Domain Objects and invokes the business logic within the
 * domain layer (either via services or via the domain objects themselves).
 */
@Service
@AllArgsConstructor
@Slf4j
public class MakanSessionAppService {

    // These are domain services.
    private final CreateMakanSessionService createMakanSessionService;
    private final EnrolMakanSessionService enrolMakanSessionService;
    private final SuggestMakanPlaceService suggestMakanPlaceService;
    private final DecideMakanPlaceService decideMakanPlaceService;
    private final MakanSessionRepository makanSessionRepository;

    public MakanSession getMakanSessionById(String makanSessionId) throws MakanSessionIDNotFoundException {
        final MakanSession makanSession = makanSessionRepository.findById(makanSessionId);
        if (makanSession == null) {
            throw new MakanSessionIDNotFoundException(makanSessionId + " cannot be found");
        }
        return makanSession;
    }

    public MakanSession createMakanSession(String ownerId, String ownerDislpayName, String sessionDisplayName,
                                           Instant meetingTime) {

        return createMakanSessionService.createMakanSession(MakanSession.builder()
                        .id(UUID.randomUUID().toString())
                        .owner(MakanKaki.builder()
                                .displayName(ownerDislpayName)
                                .id(ownerId)
                                .build())
                        .gatherTime(meetingTime)
                        .displayName(sessionDisplayName)
                        .state(MakanSessionState.ACTIVE)
                .build());
    }


    public MakanSession enrolMakanSession(String makanSessionId, String participantId, String participantDisplayName)
            throws MakanSessionIDNotFoundException, BusinessRuleException {
        return enrolMakanSessionService.enroll(makanSessionId, MakanKaki.builder()
                        .id(participantId)
                        .displayName(participantDisplayName)
                        .build());
    }

    public MakanSession suggestMakanPlace(String makanSessionId, String participantId, String placeName)
            throws MakanSessionIDNotFoundException, BusinessRuleException {
        return suggestMakanPlaceService.makeSuggestion(makanSessionId, placeName, participantId);
    }

    public MakanSession endSession(String makanSesionId, String userId) throws MakanSessionIDNotFoundException,
            BusinessRuleException, ForbiddenOperationException {
        return decideMakanPlaceService.decideMakanPlace(makanSesionId, userId);
    }
}
