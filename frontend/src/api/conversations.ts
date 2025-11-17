import type {
  AddMemberDevicesRequest,
  CreateConversationRequest,
  CreateConversationResponse,
  DistributeCKRequest,
  SendMessageRequest,
  SendMessageResponse,
  UUID,
} from './dtos.ts';
import { httpClient } from './httpClient.ts';

export interface SendMessageContext {
  senderUserId: UUID;
  senderDeviceId: UUID;
}

export const conversationsApi = {
  create: (payload: CreateConversationRequest) =>
    httpClient.post<CreateConversationResponse>('/v1/conversations', {
      body: JSON.stringify(payload),
      headers: { 'Content-Type': 'application/json' },
    }),

  addMemberDevices: (conversationId: UUID, payload: AddMemberDevicesRequest) =>
    httpClient.post<void>(
      `/v1/conversations/${conversationId}/members/devices`,
      {
        body: JSON.stringify(payload),
        headers: { 'Content-Type': 'application/json' },
      },
    ),

  distributeControlKey: (conversationId: UUID, payload: DistributeCKRequest) =>
    httpClient.post<SendMessageResponse>(
      `/v1/conversations/${conversationId}/control/ck`,
      { body: JSON.stringify(payload) },
    ),

  sendMessage: (
    conversationId: UUID,
    payload: SendMessageRequest,
    context: SendMessageContext,
  ) =>
    httpClient.post<SendMessageResponse>(
      `/v1/conversations/${conversationId}/messages`,
      {
        body: JSON.stringify(payload),
        headers: {
          'X-User-Id': context.senderUserId,
          'X-Device-Id': context.senderDeviceId,
        },
      },
    ),
};
