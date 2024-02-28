package fucai.me.makanplace.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class EnrollMakanSessionRequest {
    private String userDisplayName;

}
