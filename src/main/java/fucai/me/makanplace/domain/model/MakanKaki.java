package fucai.me.makanplace.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class MakanKaki {
    @NonNull
    private String id;
    @NonNull
    private String displayName;

}
