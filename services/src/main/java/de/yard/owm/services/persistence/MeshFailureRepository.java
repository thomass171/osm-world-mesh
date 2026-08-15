package de.yard.owm.services.persistence;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

//no remote access intended @CrossOrigin
@Repository
public interface MeshFailureRepository extends PagingAndSortingRepository<PersistedMeshFailure, Long>, ListCrudRepository<PersistedMeshFailure, Long> {

    @RestResource(exported = false)
    public void deleteByPersistedMesh(@Param("mesh") PersistedMesh mesh);


}
