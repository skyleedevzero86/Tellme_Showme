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
        <div className="app-shell">
          <nav className="top-nav">
            <Link className="nav-link" href="/">
              홈
            </Link>
            <Link className="nav-link" href="/webhook">
              웹후크
            </Link>
            <Link className="nav-link" href="/get-updates">
              Long Polling
            </Link>
            <Link className="nav-link" href="/channel">
              채널 브로드캐스트
            </Link>
            <Link className="nav-link" href="/channel/history">
              받은 메시지 이력
            </Link>
          </nav>
          <main className="page-container">{children}</main>
        </div>
      </body>
    </html>
  );
}
