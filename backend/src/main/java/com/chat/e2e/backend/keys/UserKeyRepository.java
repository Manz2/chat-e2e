package com.chat.e2e.backend.keys;

import com.chat.e2e.backend.device.UserDevice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserKeyRepository extends JpaRepository<UserKey, UUID> {

    @Query("""
      select k from UserKey k
      where k.device = :device and k.type = com.chat.e2e.backend.keys.UserKeyType.signed_prekey
      order by k.createdAt desc
    """)
    List<UserKey> findSpks(UserDevice device);

    @Query("""
      select count(k) from UserKey k
      where k.device = :device and k.type = com.chat.e2e.backend.keys.UserKeyType.one_time_prekey and k.used = false
    """)
    long countAvailableOpk(UserDevice device);

    // Atomarer OPK-Claim via native CTE (PostgreSQL)
    @Query(value = """
        WITH cte AS (
          SELECT id FROM user_key
          WHERE device_id = :deviceId
            AND type = 'one_time_prekey'
            AND is_used = FALSE
          ORDER BY created_at
          FOR UPDATE SKIP LOCKED
          LIMIT 1
        )
        UPDATE user_key u
        SET is_used = TRUE, claimed_at = now()
        FROM cte
        WHERE u.id = cte.id
        RETURNING u.id, u.key_id, u.public_key
      """, nativeQuery = true)
    List<Object[]> claimOneOpk(@Param("deviceId") UUID deviceId);
}
