package de.yard.owm.services.persistence;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Repository
public interface MeshPolygonNodeRepository extends PagingAndSortingRepository<PersistedMeshPolygonNode, Long>, ListCrudRepository<PersistedMeshPolygonNode, Long> {

    void deleteAll();

}
