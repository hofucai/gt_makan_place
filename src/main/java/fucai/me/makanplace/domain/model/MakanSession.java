package fucai.me.makanplace.domain.model;

import com.google.common.collect.ImmutableMap;
import fucai.me.makanplace.domain.enumeration.MakanSessionState;
import fucai.me.makanplace.domain.exception.business.BusinessRuleException;
import fucai.me.makanplace.domain.exception.business.DuplicatedDisplayNameInSessionException;
import fucai.me.makanplace.domain.exception.business.KakiNotEnrolledException;
import fucai.me.makanplace.domain.exception.business.MakanSesssionClosedException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class MakanSession {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    @NonNull
    private String displayName;
    @NonNull
    private MakanSessionState state;
    @NonNull
    private Instant gatherTime;
    @NonNull
    private MakanKaki owner;
    @Builder.Default
    private Map<String, MakanKaki> participants = ImmutableMap.of();
    @Builder.Default
    private Map<String, MakanPlace> suggestedPlaces = ImmutableMap.of();
    @Builder.Default
    private Optional<MakanPlace> selectedPlace = Optional.empty();

    private static class InvariantEnforcingBuilder extends MakanSession.MakanSessionBuilder {
        // https://ddd-practitioners.com/home/glossary/business-invariant/

        @Override
        public MakanSession build() {
            if (super.state == MakanSessionState.ACTIVE
                    && super.selectedPlace$set && !super.selectedPlace$value.isEmpty()) {
                throw new IllegalStateException(
                        "A session cannot be in the active state and also have a place decided.");
            }
            return super.build();
        }
    }

    public static MakanSession.MakanSessionBuilder builder() {
        return new InvariantEnforcingBuilder();
    }

    public MakanSession abort() throws MakanSesssionClosedException {
        checkSessionIsAlreadyTerminal();
        return this.toBuilder().state(MakanSessionState.ABANDONED).build();
    }

    public boolean isOwner(String makanKakiId) {
        return owner.getId().equalsIgnoreCase(makanKakiId);
    }

    public MakanSession suggestPlace(String makanKakiId, String placeName) throws
            MakanSesssionClosedException, KakiNotEnrolledException {
        checkSessionIsAlreadyTerminal();
        MakanSession.MakanSessionBuilder returnBuilder = this.toBuilder();

        final MakanKaki makanKaki =  isOwner(makanKakiId) ?
                owner : participants.get(makanKakiId);

        if (makanKaki == null) {
            throw new KakiNotEnrolledException(makanKakiId + " is not enrolled in this session.");
        }

        final Map<String, MakanPlace> effectiveSuggestedPlaces = new HashMap<>(suggestedPlaces);
        final MakanPlace existingSuggestion =  suggestedPlaces.get(placeName);
        if (existingSuggestion == null) {
            effectiveSuggestedPlaces.put(placeName,
                    MakanPlace.builder()
                        .placeName(placeName)
                        .suggesters(ImmutableMap.of(makanKaki.getId(), makanKaki))
                    .build()
            );
        } else {
            final Map<String, MakanKaki> effectiveSuggestors = new HashMap<>(existingSuggestion.getSuggesters());
            effectiveSuggestors.put(makanKaki.getId(), makanKaki);
            effectiveSuggestedPlaces.put(placeName,
                    existingSuggestion.toBuilder().suggesters(effectiveSuggestors).build());
        }
        return returnBuilder.suggestedPlaces(ImmutableMap.copyOf(effectiveSuggestedPlaces)).build();
    }

    public MakanSession decide(DecisionPolicy decisionPolicy) throws MakanSesssionClosedException {
        checkSessionIsAlreadyTerminal();
        if (suggestedPlaces.isEmpty()) {
            // if a "decide" is called on a session with no suggestions.. then
            // effectively, we're just abandoning the session.
            abort();
        }
        final MakanPlace selectedPlace = decisionPolicy.decide(this);
        return this.toBuilder()
                .selectedPlace(Optional.of(selectedPlace))
                .state(MakanSessionState.DECIDED)
                .build();
    }

    private void checkSessionIsAlreadyTerminal() throws MakanSesssionClosedException {
        if (state == MakanSessionState.ABANDONED) {
            throw new MakanSesssionClosedException("Makan session was abandoned");
        } else if (state == MakanSessionState.DECIDED) {
            throw new MakanSesssionClosedException("Makan session already decided");
        }
    }

    public MakanSession enroll(MakanKaki makanKaki) throws BusinessRuleException {

        checkSessionIsAlreadyTerminal();

        if (isOwner(makanKaki.getId())) {
            return this;
        }

        final Set<String> existingDisplayNames = participants.values()
                .stream().map(x -> x.getDisplayName()).collect(Collectors.toSet());
        existingDisplayNames.add(owner.getDisplayName());

        if (existingDisplayNames.contains(makanKaki.getDisplayName())) {
            throw new DuplicatedDisplayNameInSessionException(
                    "There is another user with the same display name: " +
                    makanKaki.getDisplayName() +
                    " in the session");
        }

        final Map<String, MakanKaki> effectiveParticipants = new HashMap<>(participants);
        effectiveParticipants.put(makanKaki.getId(), makanKaki);
        return this.toBuilder().participants(ImmutableMap.copyOf(effectiveParticipants)).build();
    }

    public MakanSession withdraw(String participantId) throws MakanSesssionClosedException {
        checkSessionIsAlreadyTerminal();

        if (participantId.equals(owner.getId())) {
            return abort();
        }

        final Map<String, MakanKaki> resultingParticipants = new HashMap<>(participants);
        resultingParticipants.remove(participantId);


        final Map<String, MakanPlace> effectivePlaces = new HashMap<>();
        for (final MakanPlace makanPlace : suggestedPlaces.values()) {
            final Map<String, MakanKaki> suggestorMap = new HashMap<>(makanPlace.getSuggesters());
            if (!suggestorMap.containsKey(participantId)) {
                effectivePlaces.put(makanPlace.getPlaceName(), makanPlace);
                continue;
            }

            if (suggestorMap.size() == 1) { // this place is only suggested by the participant who withdrew
                continue;
            }

            suggestorMap.remove(participantId);
            effectivePlaces.put(makanPlace.getPlaceName(), makanPlace.toBuilder()
                            .suggesters(ImmutableMap.copyOf(suggestorMap))
                        .build());
        }
        return this.toBuilder()
                .participants(resultingParticipants)
                .suggestedPlaces(effectivePlaces)
                .build();
    }

}
