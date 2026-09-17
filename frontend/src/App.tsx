import { Button } from '@/components/ui/button'
import { LoginPage } from '@/components/LoginPage'
import { KpiValidationDashboard } from '@/components/kpi/KpiValidationDashboard'
import { useAuth } from '@/hooks/useAuth'

function App() {
  const { user, isLoading, logout } = useAuth()

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

  return (
    <div className="flex min-h-svh flex-col">
      <div className="border-border flex items-center justify-between border-b p-4">
        <div>
          <h1 className="text-lg font-semibold">dpm_monorepo — KPI Validation</h1>
          <p className="text-muted-foreground text-xs">Signed in as {user.email}</p>
        </div>
        <Button variant="outline" onClick={() => void logout()}>
          Sign out
        </Button>
      </div>
      <KpiValidationDashboard />
    </div>
  )
}

export default App
