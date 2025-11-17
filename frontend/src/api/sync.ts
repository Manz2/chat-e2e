import type { BootstrapResponse } from './dtos.ts';
import { httpClient } from './httpClient.ts';

export const syncApi = {
  bootstrap: () => httpClient.get<BootstrapResponse>('/v1/sync/bootstrap'),
};
