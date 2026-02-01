package com.profile.rescue.demo.repository;

import com.profile.rescue.demo.entity.PetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<PetEntity, UUID> {

    List<PetEntity> findByOwnerId(UUID ownerId);

    List<PetEntity> findByOwnerFirebaseUid(String firebaseUid);
}
