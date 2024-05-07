package de.yard.owm.services.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin
@Repository
public interface OsmWayRepository extends PagingAndSortingRepository<PersistedOsmWay, Long> {

    List<PersistedOsmWay> findAll();
}
