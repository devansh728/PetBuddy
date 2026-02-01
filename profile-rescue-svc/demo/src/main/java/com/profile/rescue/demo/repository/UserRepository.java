package com.profile.rescue.demo.repository;

import com.profile.rescue.demo.entity.UserEntity;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByFirebaseUid(String firebaseUid);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByEmail(String email);

    /**
     * Find volunteers within a given radius from a location.
     * Uses PostGIS ST_DWithin for efficient spatial query with GIST index.
     * Note: Using CAST() syntax to avoid parameter parsing issues with ::
     * 
     * @param location     The incident location (Point)
     * @param radiusMeters Search radius in meters
     * @return List of volunteers within the radius
     */
    @Query(value = """
            SELECT * FROM users u
            WHERE u.is_volunteer = true
            AND u.location IS NOT NULL
            AND ST_DWithin(
                CAST(u.location AS geography),
                CAST(:location AS geography),
                :radiusMeters
            )
            ORDER BY ST_Distance(CAST(u.location AS geography), CAST(:location AS geography)) ASC
            """, nativeQuery = true)
    List<UserEntity> findVolunteersWithinRadius(
            @Param("location") Point location,
            @Param("radiusMeters") double radiusMeters);

    /**
     * Count volunteers within radius (for quick check).
     */
    @Query(value = """
            SELECT COUNT(*) FROM users u
            WHERE u.is_volunteer = true
            AND u.location IS NOT NULL
            AND ST_DWithin(
                CAST(u.location AS geography),
                CAST(:location AS geography),
                :radiusMeters
            )
            """, nativeQuery = true)
    long countVolunteersWithinRadius(
            @Param("location") Point location,
            @Param("radiusMeters") double radiusMeters);

    /**
     * Find all active volunteers with location data.
     */
    List<UserEntity> findByIsVolunteerTrueAndLocationIsNotNull();
}
