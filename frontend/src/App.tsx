import { LoginPage } from '@/components/LoginPage'
import { DashboardPage } from '@/components/dashboard/DashboardPage'
import { CoachTourProvider } from '@/components/dashboard/CoachMarks'
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

  return (
    <CoachTourProvider>
      <DashboardPage />
    </CoachTourProvider>
  )
}

export default App
