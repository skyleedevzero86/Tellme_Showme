'use client';

import { useEffect } from 'react';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div style={{ padding: 16 }}>
      <h2>오류가 발생했습니다</h2>
      <p>{error.message || '알 수 없는 오류입니다.'}</p>
      <button type="button" onClick={reset}>
        다시 시도
      </button>
    </div>
  );
}
