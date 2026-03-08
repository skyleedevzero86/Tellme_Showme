import type {
  WebhookResponse,
  WebhookInfoResponse,
  SendMessageResponse,
  GetUpdatesResponse,
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
    if (!res.ok) throw new Error(res.statusText);
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
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },
};
