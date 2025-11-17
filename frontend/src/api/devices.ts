import type {
  DeviceSummary,
  EnrollmentFinishRequest,
  EnrollmentStartRequest,
  EnrollmentStartResponse,
  InboxQueryParams,
  InboxResponse,
  UUID,
} from './dtos.ts';
import { httpClient } from './httpClient.ts';

export interface RevokeDeviceParams {
  deviceId: UUID;
  ownerHandle: string;
}

export const deviceApi = {
  startEnrollment: (payload: EnrollmentStartRequest) =>
    httpClient.post<EnrollmentStartResponse>(
      '/v1/devices/enroll/start',
      { body: JSON.stringify(payload) },
    ),

  finishEnrollment: (deviceId: UUID, payload: EnrollmentFinishRequest) =>
    httpClient.post<void>(`/v1/devices/${deviceId}/enroll/finish`, {
      body: JSON.stringify(payload),
    }),

  revoke: ({ deviceId, ownerHandle }: RevokeDeviceParams) =>
    httpClient.post<void>(`/v1/devices/${deviceId}/revoke`, {
      headers: {
        'X-User-Handle': ownerHandle,
      },
    }),

  listPublicDevices: (handle: string, includeRevoked = false) =>
    httpClient.get<DeviceSummary[]>(
      `/v1/users/${encodeURIComponent(handle)}/devices`,
      { query: { include_revoked: includeRevoked } },
    ),

  fetchInbox: (deviceId: UUID, params: InboxQueryParams = {}) =>
    httpClient.get<InboxResponse>(`/v1/devices/${deviceId}/inbox`, {
      query: {
        since: params.since ?? undefined,
        limit: params.limit ?? undefined,
      },
    }),
};
