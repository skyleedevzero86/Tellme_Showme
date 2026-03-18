import { NextRequest, NextResponse } from 'next/server';

const BACKEND_ORIGIN = process.env.BACKEND_ORIGIN ?? 'http://localhost:8080';

export const runtime = 'nodejs';

async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  const pathStr = path.join('/');
  const url = new URL(request.url);
  const backendUrl = `${BACKEND_ORIGIN}/${pathStr}${url.search}`;

  const headers = new Headers(request.headers);
  headers.delete('host');
  headers.delete('content-length');
  headers.set('x-forwarded-host', url.host);

  try {
    const init = {
      method: request.method,
      headers,
      body: request.method === 'GET' || request.method === 'HEAD' ? undefined : request.body,
      duplex: 'half',
    } as RequestInit as any;

    const res = await fetch(backendUrl, init);

    const resHeaders = new Headers(res.headers);
    resHeaders.delete('transfer-encoding');

    return new NextResponse(res.body, {
      status: res.status,
      statusText: res.statusText,
      headers: resHeaders,
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Backend unreachable';
    return NextResponse.json(
      { error: 'Proxy failed', message },
      { status: 502 }
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
