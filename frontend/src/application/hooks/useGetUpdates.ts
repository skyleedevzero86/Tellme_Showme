'use client';

import { useCallback, useRef, useState } from 'react';
import type { GetUpdatesResponse } from '@/domain/types/api';
import { telegramApiRepository } from '@/infrastructure/api/TelegramApiRepository';

export function useGetUpdates() {
  const [status, setStatus] = useState<string>('');
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  const [lastWebhookActive, setLastWebhookActive] = useState(false);
  const abortRef = useRef(false);

  const pollOnce = useCallback(async (): Promise<GetUpdatesResponse | null> => {
    try {
      const data = await telegramApiRepository.getUpdates();
      setStatus(data.result);
      setLastUpdated(new Date());
      setLastWebhookActive(Boolean(data.webhookActive));
      return data;
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Error';
      setStatus(message);
      setLastUpdated(new Date());
      setLastWebhookActive(false);
      return null;
    }
  }, []);

  const start = useCallback(() => {
    abortRef.current = false;
    setIsPolling(true);

    const run = async () => {
      if (abortRef.current) return;
      await pollOnce();
      if (abortRef.current) return;
      setTimeout(run, 0);
    };

    run();
  }, [pollOnce]);

  const stop = useCallback(() => {
    abortRef.current = true;
    setIsPolling(false);
  }, []);

  return { status, lastUpdated, isPolling, start, stop, pollOnce, lastWebhookActive };
}
