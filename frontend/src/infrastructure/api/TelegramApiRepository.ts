import type {
  WebhookResponse,
  WebhookInfoResponse,
  SendMessageResponse,
  GetUpdatesResponse,
  MessageHistoryResponse,
  FileHistoryResponse,
} from '@/domain/types/api';
import { ENDPOINTS } from '@/domain/constants/endpoints';

const getBaseUrl = (): string =>
  typeof window === 'undefined'
    ? ''
    : process.env.NEXT_PUBLIC_API_URL ?? '';

const buildUrl = (path: string): string => {
  const base = getBaseUrl();
  if (base) return `${base.replace(/\/$/, '')}/${path}`;
  return `/api/proxy/${path}`;
};

export const telegramApiRepository = {
  async _readErrorMessage(res: Response): Promise<string> {
    const ct = res.headers.get('content-type') ?? ''
    if (ct.includes('application/json')) {
      const body = await res.json().catch(() => null)
      const msg = body?.message
      if (typeof msg === 'string' && msg.trim() !== '') return msg
    }
    return res.statusText
  },

  async getWebhookStatus(): Promise<{ webhookUrlConfigured: boolean; webhookUrl: string }> {
    const res = await fetch(buildUrl(ENDPOINTS.WEBHOOK_STATUS), { method: 'GET' });
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },

  async setWebhook(enabled: boolean, webhookUrl?: string): Promise<WebhookResponse> {
    const params = new URLSearchParams({ enabled: String(enabled) });
    if (enabled && webhookUrl) params.set('url', webhookUrl);
    const url = `${buildUrl(ENDPOINTS.WEBHOOK)}?${params.toString()}`;
    const res = await fetch(url, { method: 'GET' });
    if (!res.ok) throw new Error(res.statusText);
    const data: WebhookResponse = await res.json();
    if (data.ok === false && data.description) {
      throw new Error(data.description);
    }
    return data;
  },

  async getWebhookInfo(): Promise<WebhookInfoResponse> {
    const res = await fetch(buildUrl(ENDPOINTS.WEBHOOK_INFO), { method: 'GET' });
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },

  async deleteWebhook(): Promise<WebhookResponse> {
    const res = await fetch(buildUrl(ENDPOINTS.WEBHOOK_DELETE), { method: 'GET' });
    if (!res.ok) throw new Error(res.statusText);
    const data: WebhookResponse = await res.json();
    if (data.ok === false && data.description) {
      throw new Error(data.description);
    }
    return data;
  },

  async getUpdates(): Promise<GetUpdatesResponse> {
    const res = await fetch(buildUrl(ENDPOINTS.GET_UPDATES), {
      method: 'GET',
    });
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },

  async sendMessage(message: string): Promise<SendMessageResponse> {
    const form = new URLSearchParams({ message });
    const res = await fetch(buildUrl(ENDPOINTS.SEND_MESSAGE), {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form.toString(),
    });
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },

  async uploadPhoto(file: File, caption?: string): Promise<SendMessageResponse> {
    const formData = new FormData();
    formData.append('filename', file);
    if (caption != null && caption.trim() !== '') {
      formData.append('caption', caption.trim().slice(0, 1024));
    }
    const res = await fetch(buildUrl(ENDPOINTS.FILE_UPLOAD), {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      throw new Error(await this._readErrorMessage(res));
    }
    return res.json();
  },

  async uploadDocument(file: File, caption?: string): Promise<SendMessageResponse> {
    const formData = new FormData();
    formData.append('filename', file);
    if (caption != null && caption.trim() !== '') {
      formData.append('caption', caption.trim().slice(0, 1024));
    }
    const res = await fetch(buildUrl(ENDPOINTS.DOCUMENT_UPLOAD), {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      throw new Error(await this._readErrorMessage(res));
    }
    return res.json();
  },

  async getMessageHistory(
    page: number = 0,
    size: number = 20,
    search?: string
  ): Promise<MessageHistoryResponse> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (search != null && search.trim() !== '') params.set('search', search.trim());
    const res = await fetch(`${buildUrl(ENDPOINTS.MESSAGE_HISTORY)}?${params.toString()}`);
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },

  async getFileHistory(
    page: number = 0,
    size: number = 20,
    search?: string
  ): Promise<FileHistoryResponse> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (search != null && search.trim() !== '') params.set('search', search.trim());
    const res = await fetch(`${buildUrl(ENDPOINTS.FILE_HISTORY)}?${params.toString()}`);
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },
};
