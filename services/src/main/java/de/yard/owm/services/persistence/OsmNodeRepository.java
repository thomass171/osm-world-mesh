package de.yard.owm.services.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Repository
public interface OsmNodeRepository extends PagingAndSortingRepository<PersistedOsmNode, Long> {

    PersistedOsmNode findByOsmId(long osmId);
}
