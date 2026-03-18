'use client';

import dynamic from 'next/dynamic';

const ChannelBroadcastPanel = dynamic(
  () =>
    import('@/presentation/components/ChannelBroadcastPanel').then((m) => ({
      default: m.ChannelBroadcastPanel,
    })),
  { ssr: false, loading: () => <p>로딩 중...</p> }
);

export function ChannelClient() {
  return <ChannelBroadcastPanel />;
}

