import { httpClient } from './httpClient.ts';
import type {
  AppUser,
  DeviceSummary,
  RegisterUserRequest,
  UserResponse,
} from './dtos.ts';

export const usersApi = {
  list: () => httpClient.get<UserResponse[]>('/v1/users'),

  getByHandle: (handle: string) =>
    httpClient.get<AppUser>(`/v1/users/${encodeURIComponent(handle)}`),

  register: (payload: RegisterUserRequest) =>
    httpClient.post<AppUser>('/v1/users/register', { body: JSON.stringify(payload) }),

  listDevices: (handle: string, includeRevoked = false) =>
    httpClient.get<DeviceSummary[]>(
      `/v1/users/${encodeURIComponent(handle)}/devices`,
      {
        query: {
          include_revoked: includeRevoked,
        },
      },
    ),
};
