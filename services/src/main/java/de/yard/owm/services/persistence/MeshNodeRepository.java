package de.yard.owm.services.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Repository
public interface MeshNodeRepository extends PagingAndSortingRepository<PersistedMeshNode, Long> {
/*
    List<Maze> findByCreatedBy(@Param("createdBy") String name);

    @RestResource(exported = true)
    Maze findByName(@Param("name") String name);*/
}
