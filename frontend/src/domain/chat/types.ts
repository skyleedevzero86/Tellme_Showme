export type ChatRole = 'user' | 'assistant';

export interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  createdAt: Date;
}

export const createChatMessage = (
  role: ChatRole,
  content: string,
  id?: string
): ChatMessage => ({
  id: id ?? `msg-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
  role,
  content,
  createdAt: new Date(),
});
