package fucai.me.makanplace.api.dto;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Value
@Builder
@Jacksonized
public class SuggestedMakanPlace {
    @NonNull
    private final String makanPlace;
    @NonNull
    private final Set<String> suggester;
}
