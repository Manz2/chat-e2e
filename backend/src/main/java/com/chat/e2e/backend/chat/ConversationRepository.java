package com.chat.e2e.backend.chat;

import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {}

