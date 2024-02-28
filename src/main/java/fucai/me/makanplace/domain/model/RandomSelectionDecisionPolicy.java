package fucai.me.makanplace.domain.model;

import lombok.Value;

import java.util.ArrayList;
import java.util.Random;

@Value
public class RandomSelectionDecisionPolicy implements DecisionPolicy {

    private final Random random;

    public RandomSelectionDecisionPolicy() {
        random = new Random();
    }

    protected RandomSelectionDecisionPolicy(Random random) {
        this.random = random;
    }

    @Override
    public MakanPlace decide(MakanSession makanSession) {
        final ArrayList<MakanPlace> makanPlaces =  new ArrayList<>(makanSession.getSuggestedPlaces().values());
        return makanPlaces.get(random.nextInt(makanPlaces.size()));
    }
}
