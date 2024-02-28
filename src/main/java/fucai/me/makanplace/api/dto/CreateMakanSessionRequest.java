package fucai.me.makanplace.api.dto;


import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Data
@Builder
@Jacksonized
public class CreateMakanSessionRequest {
    private String userDisplayName;
    private String makanSessionDisplayName;
    private Instant gatherTime;

}
