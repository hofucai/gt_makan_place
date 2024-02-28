package fucai.me.makanplace.api;

import fucai.me.makanplace.api.dto.MakanSessionResource;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * This class is used to generate the spring-hateoas HAL links on the API's DTO.
 */
@Data
@Accessors(fluent = true)
public class HATEOASBuilder {


    private MakanSessionResource dto;
    private boolean selfLink;
    private boolean enrollLink;
    private boolean suggestPlaceLink;
    private boolean decidePlaceLink;

    public HATEOASBuilder(MakanSessionResource dto) {
        this.dto = dto;
    }

    public RepresentationModel build() {

        if (selfLink) {
            dto.add(Affordances.of(
                        linkTo(methodOn(MakanSessionController.class).get(null,
                        dto.getMakanSessionId())).withSelfRel())
                            .afford(HttpMethod.GET)
                    .toLink()
            );
        }

        if (enrollLink) {
            dto.add(
                    Affordances.of(linkTo(methodOn(MakanSessionController.class)
                        .enrollUser(null, dto.getMakanSessionId(), null))
                        .withRel("enroll"))
                            .afford(HttpMethod.POST)
                            .toLink()
                    );
        }

        if (suggestPlaceLink) {
            dto.add(Affordances.of(
                    linkTo(methodOn(MakanSessionController.class)
                    .suggestPlace(null, dto.getMakanSessionId(), null))
                    .withRel("suggest-place"))
                    .afford(HttpMethod.POST)
                    .toLink());
        }

        if (decidePlaceLink) {
            dto.add(Affordances.of(linkTo(methodOn(MakanSessionController.class)
                    .decidePlace(null, dto.getMakanSessionId()))
                    .withRel("decide"))
                    .afford(HttpMethod.POST)
                    .toLink());
        }

        return dto;

    }





}
