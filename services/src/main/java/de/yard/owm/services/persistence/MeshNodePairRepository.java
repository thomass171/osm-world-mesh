package de.yard.owm.services.persistence;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Repository
public interface MeshNodePairRepository extends PagingAndSortingRepository<PersistedMeshNodePair, Long>, ListCrudRepository<PersistedMeshNodePair, Long> {

}
