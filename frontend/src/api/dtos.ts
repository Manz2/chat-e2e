export type UUID = string;
export type ISODateString = string;

export interface DeviceSummary {
  deviceId: UUID;
  deviceName: string;
  platform: string;
  createdAt: ISODateString;
  lastSeenAt: ISODateString;
  revoked: boolean;
}

export interface EnrollmentStartRequest {
  userHandle: string;
}

export interface EnrollmentStartResponse {
  deviceId: UUID;
  nonce: string;
  expiresAt: ISODateString;
}

export interface EnrollmentFinishRequest {
  ikPub: string;
  kxPub: string;
  bindingSig: string;
  proof: string;
  platform?: string;
  deviceName?: string;
}

export interface RegisterUserRequest {
  handle: string;
  displayName: string;
  password: string;
}

export interface CreateConversationRequest {
  isGroup: boolean;
  memberHandles: string[];
}

export interface CreateConversationResponse {
  conversationId: UUID;
  createdAt: ISODateString;
}

export interface AddMemberDevicesRequest {
  handle: string;
  deviceIds: UUID[];
}

export interface DistributeCKRequest {
  epoch: number;
  sealedForDevice: Record<UUID, string>;
  sigFromDevice?: string;
  fromDeviceId: UUID;
}

export interface SendMessageRequest {
  contentType: string;
  epoch: number;
  counter: number;
  ciphertextB64: string;
}

export interface SendMessageResponse {
  messageId: UUID;
  createdAt: ISODateString;
  deliveries: number;
}

export interface BootstrapResponse {
  userId: UUID;
  handle: string;
  conversations: ConversationBrief[];
  devices: UserDeviceBrief[];
}

export interface ConversationBrief {
  conversationId: UUID;
  isGroup: boolean;
  createdAt: ISODateString;
  members: MemberBrief[];
}

export interface MemberBrief {
  userId: UUID;
  handle: string;
}

export interface UserDeviceBrief {
  deviceId: UUID;
  platform: string;
  revokedAt: ISODateString | null;
  lastSeenAt: ISODateString | null;
}

export interface InboxResponse {
  items: DeliveryDTO[];
  nextCursor: string | null;
}

export interface DeliveryDTO {
  deliveryId: UUID;
  messageId: UUID;
  conversationId: UUID;
  contentType: string;
  msgHeaderJson: string;
  ciphertextB64: string;
  createdAt: ISODateString;
}

export interface AckRequest {
  deviceId: UUID;
  deliveryIds: UUID[];
}

export interface ReadRequest {
  deviceId: UUID;
  messageId: UUID;
}

export interface ReadWsMessage {
  conversationId: UUID;
  messageId: UUID;
}

export interface SendWsMessage {
  conversationId: UUID;
  contentType: string;
  epoch: number;
  counter: number;
  ciphertextB64: string;
}

export interface SendAckEvent {
  messageId: UUID;
  conversationId: UUID;
  createdAt: ISODateString;
  deliveries: number;
}

export interface ReadEvent {
  conversationId: UUID;
  messageId: UUID;
  byDeviceId: UUID;
  at: ISODateString;
}

export interface DeliveredEvent {
  messageId: UUID;
  conversationId: UUID;
  recipientDeviceId: UUID;
  createdAt: ISODateString;
}

export interface JwtClaims {
  userId: UUID;
  deviceId: UUID;
}

export interface InboxQueryParams {
  since?: string | null;
  limit?: number;
}

export interface AppUser {
  id: UUID;
  handle: string;
  displayName: string;
  passwordHash: string;
  twoFaEnabled: boolean;
  createdAt: ISODateString;
}

export interface UserResponse {
  id: UUID;
  handle: string;
  displayName: string;
  twoFaEnabled: boolean;
  createdAt: ISODateString;
}
