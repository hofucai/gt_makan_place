package fucai.me.makanplace.api.dto;

import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;
import java.util.Set;

/**
 * Simply the DTO for the response of all the APIs.
 */
@Value
@Builder
@Jacksonized
public class MakanSessionResource extends RepresentationModel<MakanSessionResource> {

    @NonNull
    private String makanSessionId;
    @NonNull
    private String displayName;
    @NonNull
    private String ownerName;
    @Builder.Default
    private Set<String> participants = ImmutableSet.of();
    @Builder.Default
    private Set<SuggestedMakanPlace> suggestedMakanPlaces = ImmutableSet.of();
    @NonNull
    private ExternalMakanSessionState state;

    private SuggestedMakanPlace selectedMakanPlace;

    @NonNull
    private Instant gatherTime;

}
