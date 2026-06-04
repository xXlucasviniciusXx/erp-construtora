import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '@/auth/AuthContext'
import { LicensingProvider } from '@/licensing/LicensingContext'
import { SettingsProvider } from '@/theme/SettingsContext'
import { ToastProvider } from '@/components/Toast'
import { ConfirmProvider } from '@/components/Confirm'
import { App } from './App'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <SettingsProvider>
        <ToastProvider>
          <ConfirmProvider>
            <BrowserRouter>
              <AuthProvider>
                <LicensingProvider>
                  <App />
                </LicensingProvider>
              </AuthProvider>
            </BrowserRouter>
          </ConfirmProvider>
        </ToastProvider>
      </SettingsProvider>
    </QueryClientProvider>
  </React.StrictMode>,
)
