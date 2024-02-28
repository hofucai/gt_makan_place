package fucai.me.makanplace.domain.service;

import fucai.me.makanplace.domain.exception.MakanSessionIDNotFoundException;
import fucai.me.makanplace.domain.exception.business.BusinessRuleException;
import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class SuggestMakanPlaceService {

    private final MakanSessionRepository makanSessionRepository;

        public MakanSession makeSuggestion(String makanSessionId, String makanPlaceName, String makanKakiId)
                throws MakanSessionIDNotFoundException, BusinessRuleException {

            final MakanSession mSession = makanSessionRepository.findById(makanSessionId);
            if (mSession == null) {
                throw new MakanSessionIDNotFoundException(makanSessionId + " is not found");
            }

            return makanSessionRepository.save(mSession.suggestPlace(makanKakiId, makanPlaceName));
        }



}
