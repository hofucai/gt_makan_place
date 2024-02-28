package fucai.me.makanplace.domain.service;

import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class CreateMakanSessionService {

    private final MakanSessionRepository makanSessionRepository;
    private final UserEnrollmentChangeService userEnrollmentChangeService;

    public MakanSession createMakanSession(MakanSession makanSession) {
        MakanSession session = makanSession;
        final String ownerId = makanSession.getOwner().getId();
        userEnrollmentChangeService.deregisterPreviousSessions(ownerId);
        return makanSessionRepository.save(session);
    }



}
