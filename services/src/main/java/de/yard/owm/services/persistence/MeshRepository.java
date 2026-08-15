package de.yard.owm.services.persistence;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin
@Repository
public interface MeshRepository extends PagingAndSortingRepository<PersistedMesh, Long>, ListCrudRepository<PersistedMesh, Long> {
/*
    List<Maze> findByCreatedBy(@Param("createdBy") String name);*/

    @RestResource(exported = true)
    PersistedMesh findByName(@Param("name") String name);


}
