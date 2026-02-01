package com.profile.rescue.demo.repository;

import com.profile.rescue.demo.entity.IncidentEntity;
import com.profile.rescue.demo.entity.IncidentEntity.IncidentStatus;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    /**
     * Find incidents within a given radius from a location.
     * Uses PostGIS ST_DWithin for efficient spatial query.
     * Note: Cast using CAST() syntax to avoid parameter parsing issues with ::
     */
    @Query(value = """
            SELECT * FROM incidents i
            WHERE ST_DWithin(
                CAST(i.location AS geography),
                CAST(:location AS geography),
                :radiusMeters
            )
            ORDER BY i.created_at DESC
            """, nativeQuery = true)
    List<IncidentEntity> findIncidentsWithinRadius(
            @Param("location") Point location,
            @Param("radiusMeters") double radiusMeters);

    /**
     * Find open incidents within radius (active rescue needed).
     */
    @Query(value = """
            SELECT * FROM incidents i
            WHERE i.status = 'OPEN'
            AND ST_DWithin(
                CAST(i.location AS geography),
                CAST(:location AS geography),
                :radiusMeters
            )
            ORDER BY i.created_at DESC
            """, nativeQuery = true)
    List<IncidentEntity> findOpenIncidentsWithinRadius(
            @Param("location") Point location,
            @Param("radiusMeters") double radiusMeters);

    List<IncidentEntity> findByReporterId(UUID reporterId);

    List<IncidentEntity> findByStatus(IncidentStatus status);

    List<IncidentEntity> findByStatusOrderByCreatedAtDesc(IncidentStatus status);
}
