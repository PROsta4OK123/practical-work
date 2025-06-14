import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Логируем API запросы для отладки
  if (pathname.startsWith('/api/')) {
    console.log(`[Middleware] ${request.method} ${pathname} -> http://localhost:8080${pathname}`)
  }

  // Пропускаем все API запросы - они будут обработаны через rewrites
  return NextResponse.next()
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
} 