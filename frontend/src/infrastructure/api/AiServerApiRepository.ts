const AI_SERVER_BASE_URL =
  process.env.NEXT_PUBLIC_AISERVER_URL?.replace(/\/$/, '') ?? 'http://localhost:6060';

const CONNECTED_EVENT_MESSAGE = '연결됨';
const FINISH_EVENT_MESSAGE = '완료';

const buildAiServerUrl = (path: string): string => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${AI_SERVER_BASE_URL}${normalizedPath}`;
};

export const aiServerApiRepository = {
  buildSseUrl(userId: string): string {
    const params = new URLSearchParams({ userId });
    return `${buildAiServerUrl('/sse/connect')}?${params.toString()}`;
  },

  async sendChatAi(currentUserName: string, message: string): Promise<Response> {
    return fetch(buildAiServerUrl('/chat/ai'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ currentUserName, message }),
    });
  },

  isIgnorableChunk(chunk: string): boolean {
    const normalized = chunk.trim();
    return (
      normalized === '' ||
      normalized.toLowerCase() === 'null' ||
      normalized === CONNECTED_EVENT_MESSAGE ||
      normalized === FINISH_EVENT_MESSAGE
    );
  },
};
