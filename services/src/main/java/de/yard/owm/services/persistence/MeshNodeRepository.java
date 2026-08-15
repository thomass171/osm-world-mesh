package de.yard.owm.services.persistence;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

//not exported @CrossOrigin
@Repository
public interface MeshNodeRepository extends PagingAndSortingRepository<PersistedMeshNode, Long>, ListCrudRepository<PersistedMeshNode, Long> {

    @RestResource(exported = false)
    void deleteByPersistedMesh(@Param("persistedMesh") PersistedMesh persistedMesh);
}
