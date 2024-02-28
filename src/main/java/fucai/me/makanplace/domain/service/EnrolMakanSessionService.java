package fucai.me.makanplace.domain.service;

import fucai.me.makanplace.domain.exception.MakanSessionIDNotFoundException;
import fucai.me.makanplace.domain.exception.business.BusinessRuleException;
import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanKaki;
import fucai.me.makanplace.domain.model.MakanSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
@AllArgsConstructor
public class EnrolMakanSessionService {

    private final MakanSessionRepository makanSessionRepository;
    private final UserEnrollmentChangeService userEnrollmentChangeService;

    public MakanSession enroll(String makanSessionId, MakanKaki kaki) throws MakanSessionIDNotFoundException,
            BusinessRuleException {
        final MakanSession sessionToEnrol = makanSessionRepository.findById(makanSessionId);
        if (sessionToEnrol == null) {
            throw new MakanSessionIDNotFoundException(makanSessionId + " does not exist");
        }
        userEnrollmentChangeService.deregisterPreviousSessions(kaki.getId());
        return makanSessionRepository.save(sessionToEnrol.enroll(kaki));
    }

}
