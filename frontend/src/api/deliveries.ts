import type { AckRequest, ReadRequest } from './dtos.ts';
import { httpClient } from './httpClient.ts';

export const deliveriesApi = {
  acknowledge: (payload: AckRequest) =>
    httpClient.post<void>('/v1/deliveries/ack', { body: JSON.stringify(payload) }),

  markRead: (payload: ReadRequest) =>
    httpClient.post<void>('/v1/deliveries/read', { body: JSON.stringify(payload) }),
};
