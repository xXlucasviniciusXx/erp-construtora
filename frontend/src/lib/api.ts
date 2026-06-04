import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

export const api = axios.create({ baseURL })

/**
 * Resolve a URL de um asset (ex.: logo) para um endereço absoluto.
 *
 * O backend devolve URLs relativas como "/api/assets/logo". Como o frontend
 * roda em outro domínio (Vercel) que o backend (Render), uma URL relativa
 * apontaria para o domínio errado. Aqui prefixamos com a origem da API.
 * URLs já absolutas (http/https) e data URIs são retornadas inalteradas.
 */
export function assetUrl(url?: string | null): string | undefined {
  if (!url) return undefined
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) return url
  try {
    const origin = new URL(baseURL).origin   // ex.: https://...onrender.com
    return origin + (url.startsWith('/') ? url : '/' + url)
  } catch {
    return url
  }
}

const TOKEN_KEY = 'cf_token'
const REFRESH_KEY = 'cf_refresh'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function setRefreshToken(token: string | null) {
  if (token) localStorage.setItem(REFRESH_KEY, token)
  else localStorage.removeItem(REFRESH_KEY)
}

// Anexa o JWT em toda requisição
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ---- Refresh token — renovação automática em 401 ----
// Múltiplos 401 concorrentes esperam o mesmo refresh em andamento.
let refreshPromise: Promise<string> | null = null

function doLogout() {
  setToken(null)
  setRefreshToken(null)
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config

    // Apenas tenta refresh em 401 e somente uma vez por request
    if (error.response?.status === 401 && !originalRequest._retry) {
      const rt = getRefreshToken()
      if (!rt) {
        doLogout()
        return Promise.reject(error)
      }

      originalRequest._retry = true

      try {
        // Se já há um refresh em andamento, aguarda o mesmo
        if (!refreshPromise) {
          refreshPromise = axios
            .post<{ token: string; refreshToken: string }>(
              `${baseURL}/auth/refresh`,
              { refreshToken: rt },
            )
            .then((res) => {
              setToken(res.data.token)
              setRefreshToken(res.data.refreshToken)
              return res.data.token
            })
            .finally(() => { refreshPromise = null })
        }

        const newToken = await refreshPromise
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return api(originalRequest)
      } catch {
        doLogout()
        return Promise.reject(error)
      }
    }

    return Promise.reject(error)
  },
)

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? error.message
  }
  return 'Erro inesperado'
}
