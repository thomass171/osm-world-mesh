package de.yard.owm.services.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin
@Repository
public interface OsmWayNodeRepository extends PagingAndSortingRepository<PersistedOsmWayNode, Long> {

    List<PersistedOsmWayNode> findAll();
}
