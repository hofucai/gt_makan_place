package fucai.me.makanplace.domain.service;

import fucai.me.makanplace.domain.exception.business.MakanSesssionClosedException;
import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class UserEnrollmentChangeService {
    private final MakanSessionRepository makanSessionRepository;

    protected void deregisterPreviousSessions(String kakiId) {
        abandonPreviousOwnSession(kakiId);
        withdrawFromOtherSessions(kakiId);
    }

    private void withdrawFromOtherSessions(String ownerId) {
        final MakanSession prevSession = makanSessionRepository.findByMakanKakiIdAndStateIsActive(ownerId);
        if (prevSession == null) {
            return;
        }
        try {
            makanSessionRepository.save(prevSession.withdraw(ownerId));
        } catch (MakanSesssionClosedException ex) {
            log.error("Unable to withdraw from previous session", ex);
        }
    }

    private void abandonPreviousOwnSession(String ownerId) {
        final MakanSession prevSession = makanSessionRepository.findByOwnerIdAndStateIsActive(ownerId);
        if (prevSession == null) {
            return;
        }
        try {
            makanSessionRepository.save(prevSession.abort());
        } catch (MakanSesssionClosedException ex) {
            // This is not possible as only active makan sessions are fetched.
            log.error("Unable to abort previous session", ex);
        }
    }



}
