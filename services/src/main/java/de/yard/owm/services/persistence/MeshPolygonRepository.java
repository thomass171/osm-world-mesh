package de.yard.owm.services.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Collection;
import java.util.List;

@CrossOrigin
@Repository
public interface MeshPolygonRepository extends PagingAndSortingRepository<PersistedMeshPolygon, Long>, ListCrudRepository<PersistedMeshPolygon, Long> {

    @RestResource(exported = false)
    void deleteByMesh(@Param("mesh") PersistedMesh mesh);

    List<PersistedMeshPolygon> findByOsmId(long osmNodeId);

    /**
     * Much more efficient than findAll() iterating
     * We should have some kind of order, at least for test reliability
     */
    @Query("SELECT DISTINCT mp FROM PersistedMeshPolygon mp " +
            "LEFT JOIN FETCH mp.meshPolygonNodes nodes " +
            //improves performance by 50%, but breaks result set "LEFT JOIN FETCH mp.nodePairs " +
            "LEFT JOIN FETCH nodes.meshNode " +
            "WHERE mp.mesh = :mesh " +
            "ORDER BY mp.id, nodes.index")
    List<PersistedMeshPolygon> findByMesh(@Param("mesh") PersistedMesh mesh);

    /**
     * Bounding-box pre-filter: returns ids of polygons whose node bounding box
     * contains (lat, lon). This is NOT an exact point-in-polygon test — it only
     * excludes polygons that are entirely east/west/north/south of the point.
     * Follow up with exact geometry (JTS) checks on the candidates.
     */
    @Query("""
        SELECT mp
        FROM PersistedMeshPolygon mp
        JOIN mp.meshPolygonNodes mpn
        JOIN mpn.meshNode n
        WHERE mp.mesh = :mesh 
        GROUP BY mp.id
        HAVING MIN(n.lat) <= :lat AND MAX(n.lat) >= :lat
           AND MIN(n.lon) <= :lon AND MAX(n.lon) >= :lon
        """)
    List<Long> findCandidatePolygonIds(@Param("mesh") PersistedMesh mesh,
                                       @Param("lat") double lat,
                                       @Param("lon") double lon);

}
