import { ENDPOINTS } from '@/domain/constants/endpoints';

type FrontendChatResponse = {
  sessionId: string;
  reply: string;
};

const getBaseUrl = (): string =>
  typeof window === 'undefined' ? '' : process.env.NEXT_PUBLIC_API_URL ?? '';

const buildUrl = (path: string): string => {
  const base = getBaseUrl();
  if (base) return `${base.replace(/\/$/, '')}/${path}`;
  return `/api/proxy/${path}`;
};

export const aiServerApiRepository = {
  async sendChatInput(sessionId: string, message: string): Promise<FrontendChatResponse> {
    const res = await fetch(buildUrl(ENDPOINTS.CHAT_INPUT), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, message }),
    });

    const body = await res.json().catch(() => null);
    if (!res.ok) {
      const reply = typeof body?.reply === 'string' ? body.reply : res.statusText;
      throw new Error(reply || 'The chat request failed.');
    }

    return {
      sessionId: typeof body?.sessionId === 'string' ? body.sessionId : sessionId,
      reply: typeof body?.reply === 'string' ? body.reply : '',
    };
  },
};
