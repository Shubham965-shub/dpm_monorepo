import { useState, type FormEvent } from 'react'
import { isAxiosError } from 'axios'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { useAuth } from '@/hooks/useAuth'
import { initials } from '@/lib/utils'
import type { UserPublic } from '@/lib/authSchemas'

// Mirrors backend AuthDtos.MIN_NEW_PASSWORD_LENGTH - a client-side mirror so the form can reject a
// too-short password before round-tripping, not a second source of truth (the backend still
// enforces it either way).
const MIN_NEW_PASSWORD_LENGTH = 8

type ChangePasswordState = 'closed' | 'form' | 'success'

/** The avatar button's own small popover (replaces the old click-to-logout button) - a "Change
 * password" option (self-service, see useAuth's changePassword) plus "Log out" in one place,
 * matching the standard profile-menu pattern instead of a single surprising click action. */
export function ProfileMenu({ user, onLogout }: { user: UserPublic; onLogout: () => void }) {
  const { changePassword } = useAuth()
  const [open, setOpen] = useState(false)
  const [state, setState] = useState<ChangePasswordState>('closed')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function resetForm() {
    setState('closed')
    setCurrentPassword('')
    setNewPassword('')
    setConfirmPassword('')
    setError(null)
  }

  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (!next) resetForm()
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.')
      return
    }
    setIsSubmitting(true)
    setError(null)
    try {
      await changePassword(currentPassword, newPassword)
      setState('success')
    } catch (err) {
      // A 401 here can only mean AuthService.changePassword rejected currentPassword - a missing/
      // expired session 401 never reaches this far (AuthenticationFilter would have already failed
      // the request before the controller ran).
      setError(
        isAxiosError(err) && err.response?.status === 401
          ? 'Current password is incorrect.'
          : 'Unable to change password. Please try again.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <button
          title={user.email}
          className="flex size-7 items-center justify-center rounded-full bg-brand text-xs font-semibold"
        >
          {initials(user.fullName)}
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="text-popover-foreground">
        <div className="flex flex-col gap-0.5 border-b border-border pb-2">
          <span className="truncate text-sm font-semibold">{user.fullName}</span>
          <span className="text-muted-foreground truncate text-xs">{user.email}</span>
        </div>

        {state === 'closed' && (
          <div className="flex flex-col gap-0.5 pt-1">
            <Button variant="ghost" className="justify-start" onClick={() => setState('form')}>
              Change password
            </Button>
            <Button variant="ghost" className="justify-start" onClick={onLogout}>
              Log out
            </Button>
          </div>
        )}

        {state === 'success' && (
          <div className="flex flex-col gap-2 pt-1">
            <p className="text-sm">Password changed.</p>
            <Button size="sm" onClick={() => handleOpenChange(false)}>
              Done
            </Button>
          </div>
        )}

        {state === 'form' && (
          <form onSubmit={handleSubmit} className="flex flex-col gap-2 pt-1">
            <div className="space-y-1">
              <Label htmlFor="change-password-current">Current password</Label>
              <Input
                id="change-password-current"
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="change-password-new">New password</Label>
              <Input
                id="change-password-new"
                type="password"
                autoComplete="new-password"
                minLength={MIN_NEW_PASSWORD_LENGTH}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="change-password-confirm">Confirm new password</Label>
              <Input
                id="change-password-confirm"
                type="password"
                autoComplete="new-password"
                minLength={MIN_NEW_PASSWORD_LENGTH}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>
            {error && (
              <p role="alert" className="text-destructive text-xs">
                {error}
              </p>
            )}
            <div className="flex justify-end gap-2 pt-1">
              <Button type="button" variant="ghost" size="sm" onClick={() => setState('closed')}>
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={isSubmitting}>
                {isSubmitting ? 'Saving…' : 'Save'}
              </Button>
            </div>
          </form>
        )}
      </PopoverContent>
    </Popover>
  )
}
