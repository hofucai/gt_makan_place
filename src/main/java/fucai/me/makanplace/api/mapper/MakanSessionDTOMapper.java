package fucai.me.makanplace.api.mapper;

import com.google.common.collect.ImmutableSet;
import fucai.me.makanplace.api.dto.ExternalMakanSessionState;
import fucai.me.makanplace.api.dto.MakanSessionResource;
import fucai.me.makanplace.api.dto.SuggestedMakanPlace;
import fucai.me.makanplace.domain.model.MakanPlace;
import fucai.me.makanplace.domain.model.MakanSession;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper class for converting internal domain objects into external facing DTOs exposed by the API.
 */
@Component
public class MakanSessionDTOMapper {

    public MakanSessionResource mapToExternalDTO(MakanSession inSession) {
        return MakanSessionResource
                .builder()
                .makanSessionId(inSession.getId())
                .state(ExternalMakanSessionState.valueOf(inSession.getState().name()))
                .ownerName(inSession.getOwner().getDisplayName())
                .displayName(inSession.getDisplayName())
                .selectedMakanPlace(inSession.getSelectedPlace().isPresent() ?
                        mapToDTO(inSession.getSelectedPlace().get()) : null)
                .participants(ImmutableSet.copyOf(inSession.getParticipants().values().stream()
                        .map(x -> x.getDisplayName()).collect(Collectors.toSet())))
                .suggestedMakanPlaces(ImmutableSet.copyOf(
                        inSession.getSuggestedPlaces().values().stream()
                                .map(this::mapToDTO).collect(Collectors.toSet())
                ))
                .gatherTime(inSession.getGatherTime())
                .build();
    }

    private SuggestedMakanPlace mapToDTO(MakanPlace inPlace) {
        return SuggestedMakanPlace.builder()
                .suggester(ImmutableSet.copyOf(inPlace.getSuggesters()
                        .values().stream().map(x -> x.getDisplayName()).collect(Collectors.toSet())))
                .makanPlace(inPlace.getPlaceName()).build();
    }
}
