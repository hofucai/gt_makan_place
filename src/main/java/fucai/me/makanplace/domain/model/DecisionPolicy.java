package fucai.me.makanplace.domain.model;

public interface DecisionPolicy {
    MakanPlace decide(MakanSession makanSession);
}
