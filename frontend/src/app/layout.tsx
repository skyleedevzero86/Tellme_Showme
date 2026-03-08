import type { Metadata } from 'next';
import Link from 'next/link';
import './globals.css';

export const metadata: Metadata = {
  title: 'Tellme Showme - 텔레그램 봇',
  description: '텔레그램 Bot API 예제 (웹후크, Long Polling, 채널 브로드캐스트)',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <nav style={{ marginBottom: 24, fontSize: 14 }}>
          <Link href="/">홈</Link>
          {' | '}
          <Link href="/webhook">웹후크</Link>
          {' | '}
          <Link href="/get-updates">Long Polling</Link>
          {' | '}
          <Link href="/channel">채널 브로드캐스트</Link>
        </nav>
        <main style={{ maxWidth: 720, margin: '0 auto', padding: 16 }}>
          {children}
        </main>
      </body>
    </html>
  );
}
