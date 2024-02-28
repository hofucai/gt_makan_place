package fucai.me.makanplace.domain.interfaces.repository;

import fucai.me.makanplace.domain.model.MakanSession;

public interface  MakanSessionRepository {

    MakanSession findById(String makanSessionId);
    MakanSession findByOwnerIdAndStateIsActive(String makanKakiId);
    MakanSession findByMakanKakiIdAndStateIsActive(String makanKakiId);

    MakanSession save(MakanSession makanSession);

}
