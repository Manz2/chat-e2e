package com.chat.e2e.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.time.Instant;
import java.util.*;

public interface MessageDeliveryRepository extends JpaRepository<MessageDelivery, UUID> {
    @Query("""
  select d.id, m.id, m.conversationId, m.contentType, m.header, d.ciphertext, m.createdAt
  from MessageDelivery d
    join MessageCore m on m.id = d.messageId
  where d.recipientDeviceId = :deviceId
    and (:sinceTs is null or m.createdAt > :sinceTs
         or (m.createdAt = :sinceTs and m.id > :sinceMsgId))
  order by m.createdAt asc, m.id asc
  """)
    List<Object[]> findNextForDevice(@Param("deviceId") UUID deviceId,
                                     @Param("sinceTs") Instant sinceTs,
                                     @Param("sinceMsgId") UUID sinceMsgId,
                                     org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("""
  update MessageDelivery d
     set d.deliveredAt = :ts
   where d.id in :ids
     and d.recipientDeviceId = :deviceId
     and d.deliveredAt is null
  """)
    int bulkSetDeliveredAt(@Param("deviceId") UUID deviceId,
                           @Param("ids") List<UUID> ids,
                           @Param("ts") Instant ts);

    @Modifying
    @Query("""
  update MessageDelivery d
     set d.readAt = :ts
   where d.recipientDeviceId = :deviceId
     and d.messageId = :messageId
  """)
    int updateRead(@Param("deviceId") UUID deviceId,
                   @Param("messageId") UUID messageId,
                   @Param("ts") Instant ts);
}
