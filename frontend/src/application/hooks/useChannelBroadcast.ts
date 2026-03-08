'use client';

import { useCallback, useState } from 'react';
import type { SendMessageResponse } from '@/domain/types/api';
import { telegramApiRepository } from '@/infrastructure/api/TelegramApiRepository';

type BroadcastStatus = 'idle' | 'loading' | 'success' | 'error';

export function useChannelBroadcast() {
  const [messageStatus, setMessageStatus] = useState<BroadcastStatus>('idle');
  const [photoStatus, setPhotoStatus] = useState<BroadcastStatus>('idle');
  const [lastResult, setLastResult] = useState<SendMessageResponse | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);

  const sendMessage = useCallback(async (text: string) => {
    setMessageStatus('loading');
    setError(null);
    try {
      const data = await telegramApiRepository.sendMessage(text.trim());
      setLastResult(data);
      setLastUpdated(new Date());
      setMessageStatus(data.status === 'ok' ? 'success' : 'error');
      if (data.status !== 'ok') setError('전송에 실패했습니다.');
      return data;
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Unknown error';
      setError(msg);
      setMessageStatus('error');
      setLastUpdated(new Date());
      throw e;
    }
  }, []);

  const uploadPhoto = useCallback(async (file: File) => {
    setPhotoStatus('loading');
    setError(null);
    try {
      const data = await telegramApiRepository.uploadPhoto(file);
      setLastResult(data);
      setLastUpdated(new Date());
      setPhotoStatus(data.status === 'ok' ? 'success' : 'error');
      if (data.status !== 'ok') setError('전송에 실패했습니다.');
      return data;
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Unknown error';
      setError(msg);
      setPhotoStatus('error');
      setLastUpdated(new Date());
      throw e;
    }
  }, []);

  const reset = useCallback(() => {
    setMessageStatus('idle');
    setPhotoStatus('idle');
    setLastResult(null);
    setError(null);
  }, []);

  return {
    sendMessage,
    uploadPhoto,
    messageStatus,
    photoStatus,
    lastResult,
    lastUpdated,
    error,
    reset,
  };
}
