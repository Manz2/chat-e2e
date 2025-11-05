package com.chat.e2e.backend.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    @Query("""
      select d from UserDevice d
      where d.user.handle = :handle
        and (:includeRevoked = true or d.revokedAt is null)
      order by d.createdAt asc
    """)
    List<UserDevice> findByUserHandle(@Param("handle") String handle,
                                      @Param("includeRevoked") boolean includeRevoked);

    Optional<UserDevice> findByIdAndUser_Handle(UUID id, String handle);
}
