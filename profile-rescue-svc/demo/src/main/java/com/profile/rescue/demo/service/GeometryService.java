package com.profile.rescue.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

/**
 * Utility service for geospatial operations.
 * Uses JTS (Java Topology Suite) for geometry handling.
 */
@Service
@Slf4j
public class GeometryService {

    // SRID 4326 = WGS84 coordinate system (standard GPS coordinates)
    private static final int SRID_WGS84 = 4326;

    private final GeometryFactory geometryFactory;

    public GeometryService() {
        // Create geometry factory with WGS84 SRID
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    }

    /**
     * Create a Point from latitude and longitude.
     * Note: In JTS, Point uses (x, y) which maps to (longitude, latitude)
     */
    public Point createPoint(double latitude, double longitude) {
        // JTS uses (x, y) = (longitude, latitude)
        Coordinate coordinate = new Coordinate(longitude, latitude);
        Point point = geometryFactory.createPoint(coordinate);
        point.setSRID(SRID_WGS84);

        log.debug("Created point: lat={}, lon={}, SRID={}", latitude, longitude, point.getSRID());
        return point;
    }

    /**
     * Extract latitude from a Point.
     */
    public double getLatitude(Point point) {
        return point != null ? point.getY() : 0;
    }

    /**
     * Extract longitude from a Point.
     */
    public double getLongitude(Point point) {
        return point != null ? point.getX() : 0;
    }

    /**
     * Check if a point is valid (not null, has coordinates).
     */
    public boolean isValidPoint(Point point) {
        return point != null && !point.isEmpty() && point.isValid();
    }

    /**
     * Validate latitude value.
     */
    public boolean isValidLatitude(double latitude) {
        return latitude >= -90 && latitude <= 90;
    }

    /**
     * Validate longitude value.
     */
    public boolean isValidLongitude(double longitude) {
        return longitude >= -180 && longitude <= 180;
    }

    /**
     * Validate coordinates.
     */
    public boolean isValidCoordinates(double latitude, double longitude) {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }
}
