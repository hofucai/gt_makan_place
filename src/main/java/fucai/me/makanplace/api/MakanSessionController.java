package fucai.me.makanplace.api;

import fucai.me.makanplace.api.dto.CreateMakanSessionRequest;
import fucai.me.makanplace.api.dto.EnrollMakanSessionRequest;
import fucai.me.makanplace.api.dto.MakanSessionResource;
import fucai.me.makanplace.api.dto.SuggestPlaceRequest;
import fucai.me.makanplace.api.mapper.MakanSessionDTOMapper;
import fucai.me.makanplace.app.service.MakanSessionAppService;
import fucai.me.makanplace.domain.exception.ForbiddenOperationException;
import fucai.me.makanplace.domain.exception.MakanSessionIDNotFoundException;
import fucai.me.makanplace.domain.exception.business.BusinessRuleException;
import fucai.me.makanplace.domain.model.MakanSession;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("makan-session")
public class MakanSessionController {

    private final MakanSessionDTOMapper dtoMapper;
    private final MakanSessionAppService makanSessionAppService;


    @PostMapping(produces = "application/hal+json")
    public ResponseEntity createMakanSession(@RequestBody CreateMakanSessionRequest createRequest,
                                                                   HttpSession httpSession) {

        if (createRequest.getGatherTime() == null || StringUtils.isBlank(createRequest.getMakanSessionDisplayName()) ||
                StringUtils.isBlank(createRequest.getUserDisplayName())) {
            return ResponseEntity.badRequest().build();
        }

        final MakanSession makanSession = makanSessionAppService.createMakanSession(httpSession.getId(),
                createRequest.getUserDisplayName(),
                createRequest.getMakanSessionDisplayName(), createRequest.getGatherTime());

        final MakanSessionResource dto = dtoMapper.mapToExternalDTO(makanSession);

        final HATEOASBuilder hateoasBuilder = new HATEOASBuilder(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                hateoasBuilder
                        .decidePlaceLink(true)
                        .enrollLink(true)
                        .selfLink(true)
                        .suggestPlaceLink(true)
                        .build()
        );
    }

    @PostMapping(path= "{id}/enroll" ,produces = "application/hal+json")
    public ResponseEntity enrollUser(HttpSession httpSession,
                                                           @PathVariable String id,
                                                           @RequestBody EnrollMakanSessionRequest enrollRequest) {

        if (StringUtils.isBlank(enrollRequest.getUserDisplayName())) {
            return ResponseEntity.badRequest().build();
        }

        final MakanSession makanSession;
        try {
            makanSession = makanSessionAppService.enrolMakanSession(id,
                    httpSession.getId(), enrollRequest.getUserDisplayName());
        } catch (MakanSessionIDNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessRuleException e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }

        final MakanSessionResource dto = dtoMapper.mapToExternalDTO(makanSession);
        final HATEOASBuilder hateoasBuilder = new HATEOASBuilder(dto);

        return ResponseEntity.status(HttpStatus.OK).body(
                hateoasBuilder
                        .selfLink(true)
                        .suggestPlaceLink(true)
                        .build()
        );
    }

    @PostMapping(path= "{id}/suggest-place" ,produces = "application/hal+json")
    public ResponseEntity suggestPlace(HttpSession httpSession,
                                                          @PathVariable String id,
                                                          @RequestBody SuggestPlaceRequest request) {

        if (StringUtils.isBlank(request.getPlaceName())) {
            return ResponseEntity.badRequest().build();
        }

        final MakanSession makanSession;
        try {
            makanSession = makanSessionAppService.suggestMakanPlace(id, httpSession.getId(), request.getPlaceName());
        } catch (MakanSessionIDNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessRuleException e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }

        final MakanSessionResource dto = dtoMapper.mapToExternalDTO(makanSession);
        final HATEOASBuilder hateoasBuilder = new HATEOASBuilder(dto);
        final boolean exposeDecideLink = makanSession.isOwner(httpSession.getId());

        return ResponseEntity.status(HttpStatus.OK).body(
                hateoasBuilder
                        .selfLink(true)
                        .suggestPlaceLink(true)
                        .decidePlaceLink(exposeDecideLink)
                        .build()
        );
    }

    @PostMapping(path= "{id}/decide" ,produces = "application/hal+json")
    public ResponseEntity decidePlace(HttpSession httpSession,
                                                            @PathVariable String id){

        final MakanSession makanSession;
        try {
            makanSession = makanSessionAppService.endSession(id, httpSession.getId());
        } catch (MakanSessionIDNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessRuleException e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final MakanSessionResource dto = dtoMapper.mapToExternalDTO(makanSession);
        final HATEOASBuilder hateoasBuilder = new HATEOASBuilder(dto);

        return ResponseEntity.status(HttpStatus.OK).body(
                hateoasBuilder
                        .selfLink(true)
                        .build()
        );
    }



    @GetMapping(path = "/{id}", produces = "application/hal+json")
    public ResponseEntity<RepresentationModel> get(HttpSession httpSession, @PathVariable String id) {

        final MakanSession makanSession;

        try {
            makanSession = makanSessionAppService.getMakanSessionById(id);
        } catch (MakanSessionIDNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }

        final MakanSessionResource dto = dtoMapper.mapToExternalDTO(makanSession);
        final HATEOASBuilder hateoasBuilder = new HATEOASBuilder(dto);
        final boolean exposeDecideLink = makanSession.isOwner(httpSession.getId());

        return ResponseEntity.status(HttpStatus.OK).body(
                hateoasBuilder
                        .selfLink(true)
                        .suggestPlaceLink(true)
                        .decidePlaceLink(exposeDecideLink)
                        .build());
    }


}
