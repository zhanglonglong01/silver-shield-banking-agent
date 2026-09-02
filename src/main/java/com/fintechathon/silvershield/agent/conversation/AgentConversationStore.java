package com.fintechathon.silvershield.agent.conversation;

import com.fintechathon.silvershield.agent.provider.BankingModelClient.ConversationMessage;
import com.fintechathon.silvershield.agent.provider.BankingModelClient.ConversationRole;
import com.fintechathon.silvershield.common.api.ResourceNotFoundException;
import com.fintechathon.silvershield.config.AgentProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AgentConversationStore {

    private final ConcurrentHashMap<UUID, Conversation> conversations = new ConcurrentHashMap<>();
    private final AgentProperties properties;
    private final Clock clock;

    public AgentConversationStore(AgentProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ConversationSnapshot open(UUID userId, UUID requestedConversationId) {
        Instant now = Instant.now(clock);
        if (requestedConversationId == null) {
            UUID id = UUID.randomUUID();
            conversations.put(id, new Conversation(id, userId, new ArrayList<>(), now));
            return new ConversationSnapshot(id, List.of());
        }
        Conversation conversation = conversations.get(requestedConversationId);
        if (conversation == null || !conversation.userId.equals(userId)
                || conversation.lastUpdated.isBefore(now.minus(properties.getConversationTtl()))) {
            if (conversation != null && conversation.lastUpdated.isBefore(now.minus(properties.getConversationTtl()))) {
                conversations.remove(requestedConversationId, conversation);
            }
            throw new ResourceNotFoundException();
        }
        synchronized (conversation) {
            return new ConversationSnapshot(conversation.id, List.copyOf(conversation.messages));
        }
    }

    public void append(UUID conversationId, UUID userId, String userMessage, String assistantAnswer) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null || !conversation.userId.equals(userId)) {
            throw new ResourceNotFoundException();
        }
        synchronized (conversation) {
            conversation.messages.add(new ConversationMessage(ConversationRole.USER, userMessage));
            conversation.messages.add(new ConversationMessage(ConversationRole.ASSISTANT, assistantAnswer));
            int overflow = conversation.messages.size() - properties.getMaxConversationMessages();
            if (overflow > 0) {
                conversation.messages.subList(0, overflow).clear();
            }
            conversation.lastUpdated = Instant.now(clock);
        }
    }

    public record ConversationSnapshot(UUID conversationId, List<ConversationMessage> messages) {}

    private static final class Conversation {
        private final UUID id;
        private final UUID userId;
        private final List<ConversationMessage> messages;
        private Instant lastUpdated;

        private Conversation(UUID id, UUID userId, List<ConversationMessage> messages, Instant lastUpdated) {
            this.id = id;
            this.userId = userId;
            this.messages = messages;
            this.lastUpdated = lastUpdated;
        }
    }
}
