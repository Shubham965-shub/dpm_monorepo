import { LoginPage } from '@/components/LoginPage'
import { DashboardPage } from '@/components/dashboard/DashboardPage'
import { useAuth } from '@/hooks/useAuth'

function App() {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <p className="text-muted-foreground text-sm">Loading…</p>
      </div>
    )
  }

  if (!user) {
    return <LoginPage />
  }

  return <DashboardPage />
}

export default App
