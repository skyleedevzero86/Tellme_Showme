import type {
  WebhookResponse,
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
  async setWebhook(enabled: boolean): Promise<WebhookResponse> {
    const url = `${buildUrl(ENDPOINTS.WEBHOOK)}?enabled=${enabled}`;
    const res = await fetch(url, { method: 'GET' });
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
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

  async uploadPhoto(file: File): Promise<SendMessageResponse> {
    const formData = new FormData();
    formData.append('filename', file);
    const res = await fetch(buildUrl(ENDPOINTS.FILE_UPLOAD), {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  },
};
