package fucai.me.makanplace.domain.service;

import fucai.me.makanplace.domain.exception.ForbiddenOperationException;
import fucai.me.makanplace.domain.exception.MakanSessionIDNotFoundException;
import fucai.me.makanplace.domain.exception.business.BusinessRuleException;
import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanSession;
import fucai.me.makanplace.domain.model.RandomSelectionDecisionPolicy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class DecideMakanPlaceService {

    private final MakanSessionRepository makanSessionRepository;
    public MakanSession decideMakanPlace(String makanSessionId, String kakiId) throws MakanSessionIDNotFoundException,
            BusinessRuleException, ForbiddenOperationException {
        final MakanSession sessionToDecide = makanSessionRepository.findById(makanSessionId);
        if (sessionToDecide == null) {
            throw new MakanSessionIDNotFoundException(makanSessionId + " does not exist");
        }
        if (!sessionToDecide.isOwner(kakiId)) {
            throw new ForbiddenOperationException("Non-Owner " + kakiId +
                    " attempted to decide session for makan session id" + sessionToDecide);
        }
        return makanSessionRepository.save(sessionToDecide.decide(new RandomSelectionDecisionPolicy()));
    }
}
