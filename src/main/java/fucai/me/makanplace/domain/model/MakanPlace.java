package fucai.me.makanplace.domain.model;

import com.google.common.collect.ImmutableMap;
import lombok.*;

import java.util.Map;


@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MakanPlace {

    @NonNull
    private String placeName;
    @Builder.Default
    private Map<String, MakanKaki> suggesters = ImmutableMap.of();

}
