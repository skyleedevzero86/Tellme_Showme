'use client';

import { useCallback, useState } from 'react';
import type { WebhookResponse } from '@/domain/types/api';
import { telegramApiRepository } from '@/infrastructure/api/TelegramApiRepository';

type WebhookStatus = 'idle' | 'loading' | 'success' | 'error';

export function useWebhook() {
  const [status, setStatus] = useState<WebhookStatus>('idle');
  const [result, setResult] = useState<WebhookResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const setWebhook = useCallback(async (enabled: boolean, url?: string) => {
    setStatus('loading');
    setError(null);
    setResult(null);
    try {
      const data = await telegramApiRepository.setWebhook(enabled, url);
      setResult(data);
      setStatus('success');
      return data;
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Unknown error';
      setError(message);
      setStatus('error');
      throw e;
    }
  }, []);

  const reset = useCallback(() => {
    setStatus('idle');
    setResult(null);
    setError(null);
  }, []);

  return { setWebhook, status, result, error, reset };
}
