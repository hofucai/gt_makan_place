package fucai.me.makanplace.infra.repository;

import fucai.me.makanplace.domain.enumeration.MakanSessionState;
import fucai.me.makanplace.domain.interfaces.repository.MakanSessionRepository;
import fucai.me.makanplace.domain.model.MakanSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is definitely <strong>not</strong> production grade.
 */
@Component
@Slf4j
public class MakanSessionInMemoryRepository implements MakanSessionRepository {


    private final ConcurrentHashMap<String, MakanSession> makanSessionMap = new ConcurrentHashMap<>();

    @Override
    public MakanSession findById(String makanSessionId) {
        return makanSessionMap.get(makanSessionId);
    }

    @Override
    public MakanSession findByOwnerIdAndStateIsActive(String makanKakiId) {
        // Yes this is not ideal, but when we scale this up or need persistent storage, we can swap out this
        // repo for a DB backed one
        final Optional<MakanSession> makanSessionSearch = makanSessionMap.values().stream()
                .filter(x ->
                        x.getState() == MakanSessionState.ACTIVE &&
                        x.getOwner().getId().equals(makanKakiId)).findFirst();
        return  makanSessionSearch.isEmpty() ? null : makanSessionSearch.get();
    }

    @Override
    public MakanSession findByMakanKakiIdAndStateIsActive(String makanKakiId) {
        // Yes this is not ideal, but when we scale this up or need persistent storage, we can swap out this
        // repo for a DB backed one
        final Optional<MakanSession> makanSessionSearch = makanSessionMap.values().stream()
                .filter(x ->
                        x.getState() == MakanSessionState.ACTIVE &&
                        x.getParticipants().containsKey(makanKakiId))
                .findAny();
        return  makanSessionSearch.isEmpty() ? null : makanSessionSearch.get();
    }

    @Override
    public MakanSession save(MakanSession makanSession) {
        if (StringUtils.isBlank(makanSession.getId())) {
            throw new IllegalArgumentException("Cannot save makan session with empty ID");
        }
        makanSessionMap.put(makanSession.getId(), makanSession);
        return makanSession;
    }
}
