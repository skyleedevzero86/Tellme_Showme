'use client';

import { useCallback, useRef, useState } from 'react';
import { createChatMessage, type ChatMessage } from '@/domain/chat';
import { aiServerApiRepository } from '@/infrastructure/api/AiServerApiRepository';

function generateUserId(): string {
  return `frontend-user-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

const INITIAL_MESSAGE =
  'You can use normal chat and commands like /god, /eng, and /search here. ' +
  'Bare aliases such as god, eng, and search are also recognized in the frontend. ' +
  'The /time command only returns safe guidance here and does not create Telegram alarms.';

const SEND_ERROR_MESSAGE =
  'The chat request could not be completed. Please check the backend and AiServer status.';

export function useAiChat(enabled: boolean) {
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    createChatMessage('assistant', INITIAL_MESSAGE),
  ]);
  const [isLoading, setIsLoading] = useState(false);
  const [status, setStatus] = useState<'idle' | 'connecting' | 'streaming' | 'error'>('idle');

  const userIdRef = useRef<string>(generateUserId());

  const sendMessage = useCallback(async (content: string) => {
    const text = content.trim();
    if (!enabled || !text || isLoading) {
      return;
    }

    setMessages((prev) => [...prev, createChatMessage('user', text)]);
    setIsLoading(true);
    setStatus('connecting');

    try {
      const response = await aiServerApiRepository.sendChatInput(userIdRef.current, text);
      setMessages((prev) => [...prev, createChatMessage('assistant', response.reply)]);
      setStatus('idle');
    } catch (error) {
      const message =
        error instanceof Error && error.message.trim() !== '' ? error.message : SEND_ERROR_MESSAGE;
      setMessages((prev) => [...prev, createChatMessage('assistant', message)]);
      setStatus('error');
    } finally {
      setIsLoading(false);
    }
  }, [enabled, isLoading]);

  const resetConversation = useCallback(() => {
    setIsLoading(false);
    setStatus('idle');
    setMessages([createChatMessage('assistant', INITIAL_MESSAGE)]);
    userIdRef.current = generateUserId();
  }, []);

  return {
    enabled,
    isLoading,
    messages,
    resetConversation,
    sendMessage,
    status,
    userId: userIdRef.current,
  };
}
