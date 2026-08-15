package de.yard.owm.services.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin
@Repository
public interface MeshPolygonRepository extends PagingAndSortingRepository<PersistedMeshPolygon, Long>, ListCrudRepository<PersistedMeshPolygon, Long> {

    @RestResource(exported = false)
    public void deleteByMesh(@Param("mesh") PersistedMesh mesh);

    /**
     * Much more efficient than findAll() iterating
     * @return
     */
    /*TODO @Query("SELECT mp FROM PersistedMeshPolygon mp " +
            "JOIN FETCH mp.nodes nodes " +
            "ORDER BY nodes.index")
    List<PersistedMeshPolygon> retrieveAll();*/

    List<PersistedMeshPolygon> findByOsmId(long osmNodeId);
}
