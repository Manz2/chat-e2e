export interface StompFrame {
  command: string;
  headers: Record<string, string>;
  body: string;
}

export interface StompMessageFrame extends StompFrame {
  command: 'MESSAGE';
  json<T>(): T;
}

export type MessageHandler = (frame: StompMessageFrame) => void;

const textDecoder = new TextDecoder();

export class StompClient {
  private socket?: WebSocket;
  private connected = false;
  private buffer = '';
  private subscriptionSeq = 0;
  private readonly subscriptions = new Map<string, MessageHandler>();
  private readonly receiptResolvers = new Map<string, () => void>();
  private pendingConnect?: {
    resolve: () => void;
    reject: (error: Error) => void;
  };

  private readonly url: string;
  constructor(url: string) {
    this.url = url;
  }

  isConnected() {
    return this.connected;
  }

  async connect(headers: Record<string, string> = {}): Promise<void> {
    if (this.connected) {
      return;
    }
    if (this.socket) {
      this.socket.close();
    }
    this.socket = new WebSocket(this.url);
    this.socket.onmessage = (event) => this.handleSocketMessage(event.data);
    this.socket.onclose = () => {
      this.connected = false;
      this.socket = undefined;
    };
    this.socket.onerror = () => {
      if (this.pendingConnect) {
        this.pendingConnect.reject(new Error('WebSocket connection error'));
        this.pendingConnect = undefined;
      }
    };
    await new Promise<void>((resolve, reject) => {
      this.pendingConnect = { resolve, reject };
      this.socket!.onopen = () => {
        this.sendFrame('CONNECT', {
          'accept-version': '1.1,1.2',
          'heart-beat': '10000,10000',
          ...headers,
        });
      };
    });
  }

  disconnect(): Promise<void> {
    if (!this.socket) {
      return Promise.resolve();
    }

    return new Promise((resolve) => {
      if (!this.connected) {
        this.socket?.close();
        resolve();
        return;
      }
      const receiptId = this.nextReceiptId();
      this.receiptResolvers.set(receiptId, () => {
        this.socket?.close();
        resolve();
      });
      this.sendFrame('DISCONNECT', { receipt: receiptId });
      setTimeout(() => {
        if (this.receiptResolvers.delete(receiptId)) {
          this.socket?.close();
          resolve();
        }
      }, 1000);
    });
  }

  subscribe(
    destination: string,
    handler: MessageHandler,
    headers: Record<string, string> = {},
  ) {
    const id = headers.id ?? this.nextSubscriptionId();
    this.subscriptions.set(id, handler);
    this.sendFrame('SUBSCRIBE', {
      id,
      destination,
      ack: 'auto',
      ...headers,
    });
    return id;
  }

  unsubscribe(id: string) {
    if (!this.subscriptions.has(id)) {
      return;
    }
    this.subscriptions.delete(id);
    this.sendFrame('UNSUBSCRIBE', { id });
  }

  send(
    destination: string,
    body: string | Record<string, unknown>,
    headers: Record<string, string> = {},
  ) {
    const payload =
      typeof body === 'string' ? body : JSON.stringify(body ?? {});
    this.sendFrame(
      'SEND',
      {
        destination,
        'content-type': 'application/json',
        ...headers,
      },
      payload,
    );
  }

  private handleSocketMessage(data: unknown) {
    if (typeof data === 'string') {
      this.consumeChunk(data);
    } else if (data instanceof Blob) {
      data.text().then((text) => this.consumeChunk(text));
    } else if (data instanceof ArrayBuffer) {
      this.consumeChunk(textDecoder.decode(new Uint8Array(data)));
    }
  }

  private consumeChunk(chunk: string) {
    this.buffer += chunk;
    let frameEnd = this.buffer.indexOf('\0');
    while (frameEnd !== -1) {
      const rawFrame = this.buffer.slice(0, frameEnd);
      this.buffer = this.buffer.slice(frameEnd + 1);
      if (rawFrame.trim().length > 0) {
        this.dispatchFrame(this.parseFrame(rawFrame));
      }
      frameEnd = this.buffer.indexOf('\0');
    }
  }

  private parseFrame(raw: string): StompFrame {
    const cleaned = raw.replace(/\r/g, '');
    const [commandLine, ...rest] = cleaned.split('\n');
    const command = commandLine.trim();
    const headers: Record<string, string> = {};
    const bodyLines: string[] = [];

    let reachedBody = false;
    for (const line of rest) {
      if (!reachedBody && line === '') {
        reachedBody = true;
        continue;
      }
      if (!reachedBody) {
        const idx = line.indexOf(':');
        if (idx > -1) {
          const key = line.slice(0, idx).trim();
          const value = line.slice(idx + 1).trim();
          headers[key] = value;
        }
      } else {
        bodyLines.push(line);
      }
    }

    return {
      command,
      headers,
      body: bodyLines.join('\n'),
    };
  }

  private dispatchFrame(frame: StompFrame) {
    switch (frame.command) {
      case 'CONNECTED':
        this.connected = true;
        this.pendingConnect?.resolve();
        this.pendingConnect = undefined;
        break;
      case 'MESSAGE': {
        const subscriptionId = frame.headers.subscription;
        const handler = subscriptionId
          ? this.subscriptions.get(subscriptionId)
          : undefined;
        if (handler) {
          handler({
            ...frame,
            command: 'MESSAGE',
            json: <T>() => JSON.parse(frame.body) as T,
          });
        }
        break;
      }
      case 'RECEIPT': {
        const receiptId = frame.headers['receipt-id'];
        if (receiptId) {
          const resolver = this.receiptResolvers.get(receiptId);
          if (resolver) {
            resolver();
            this.receiptResolvers.delete(receiptId);
          }
        }
        break;
      }
      case 'ERROR': {
        const error = new Error(frame.body || 'STOMP protocol error');
        if (this.pendingConnect) {
          this.pendingConnect.reject(error);
          this.pendingConnect = undefined;
        } else {
          console.error('STOMP error', error);
        }
        break;
      }
      default:
        break;
    }
  }

  private sendFrame(
    command: string,
    headers: Record<string, string> = {},
    body = '',
  ) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket is not connected.');
    }
    let frame = `${command}\n`;
    Object.entries(headers).forEach(([key, value]) => {
      frame += `${key}:${value}\n`;
    });
    frame += '\n';
    frame += body;
    frame += '\0';
    this.socket.send(frame);
  }

  private nextSubscriptionId() {
    this.subscriptionSeq += 1;
    return `sub-${this.subscriptionSeq}`;
  }

  private nextReceiptId() {
    return `receipt-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
