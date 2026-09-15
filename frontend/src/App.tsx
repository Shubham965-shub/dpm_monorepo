import { Button } from '@/components/ui/button'
import { LoginPage } from '@/components/LoginPage'
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
    <div className="flex min-h-svh flex-col items-center justify-center gap-4">
      <h1 className="text-2xl font-semibold">dpm_monorepo</h1>
      <p className="text-muted-foreground">Signed in as {user.email}</p>
      <div className="flex gap-2">
        <Button>It works</Button>
        <Button variant="outline" onClick={() => void logout()}>
          Sign out
        </Button>
      </div>
    </div>
  )
}

export default App
