import type {
  ReadEvent,
  ReadWsMessage,
  SendAckEvent,
  SendWsMessage,
  UUID,
} from '../dtos.ts';
import { appConfig } from '../../config/env.ts';
import { type MessageHandler, StompClient } from './stompClient.ts';

export interface ConversationSubscription {
  conversationId: UUID;
  subscriptionId: string;
}

export class ChatRealtimeClient {
  private readonly stomp = new StompClient(appConfig.websocketUrl);
  private deviceSubscriptionId?: string;
  private readonly conversationSubscriptions = new Map<UUID, string>();

  connect(headers: Record<string, string> = {}) {
    return this.stomp.connect(headers);
  }

  disconnect() {
    this.conversationSubscriptions.forEach((id) => this.stomp.unsubscribe(id));
    if (this.deviceSubscriptionId) {
      this.stomp.unsubscribe(this.deviceSubscriptionId);
      this.deviceSubscriptionId = undefined;
    }
    this.conversationSubscriptions.clear();
    return this.stomp.disconnect();
  }

  sendMessage(payload: SendWsMessage) {
    this.stomp.send('/app/messages.send', JSON.stringify(payload));
  }

  markRead(payload: ReadWsMessage) {
    this.stomp.send('/app/messages.read', JSON.stringify(payload));
  }

  subscribeDeviceQueue(handler: (payload: SendAckEvent) => void) {
    if (this.deviceSubscriptionId) {
      this.stomp.unsubscribe(this.deviceSubscriptionId);
    }
    this.deviceSubscriptionId = this.stomp.subscribe(
      '/user/queue/device',
      (frame) => handler(frame.json<SendAckEvent>()),
    );
    return this.deviceSubscriptionId;
  }

  subscribeConversation(conversationId: UUID, handler: (payload: ReadEvent) => void) {
    const topic = `/topic/conversation.${conversationId}`;
    const existing = this.conversationSubscriptions.get(conversationId);
    if (existing) {
      this.stomp.unsubscribe(existing);
    }
    const subscriptionId = this.stomp.subscribe(topic, (frame) =>
      handler(frame.json<ReadEvent>()),
    );
    this.conversationSubscriptions.set(conversationId, subscriptionId);
    return subscriptionId;
  }

  unsubscribeConversation(conversationId: UUID) {
    const subscriptionId = this.conversationSubscriptions.get(conversationId);
    if (subscriptionId) {
      this.stomp.unsubscribe(subscriptionId);
      this.conversationSubscriptions.delete(conversationId);
    }
  }

  onRaw(
    destination: string,
    handler: MessageHandler,
    headers: Record<string, string> = {},
  ) {
    return this.stomp.subscribe(destination, handler, headers);
  }
}
