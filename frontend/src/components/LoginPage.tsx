import { useEffect, useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/hooks/useAuth'
import { useTheme } from '@/hooks/useTheme'
import { LoginHero } from './login/LoginHero'
import './login/login.css'

// Below this width the hero art drops to 10% opacity behind the sign-in card and stops rotating
// (see login.css's matching breakpoint) - a login screen that's mid-transition when someone's
// trying to read a form on a small screen is just noise.
const NARROW_BREAKPOINT_QUERY = '(max-width: 860px)'

export function LoginPage() {
  const { login } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Pause on hover or focus in the sign-in card - never animate behind someone typing a password.
  const [signinActive, setSigninActive] = useState(false)
  const [isNarrow, setIsNarrow] = useState(
    () => typeof window !== 'undefined' && window.matchMedia(NARROW_BREAKPOINT_QUERY).matches,
  )

  useEffect(() => {
    const mql = window.matchMedia(NARROW_BREAKPOINT_QUERY)
    const onChange = () => setIsNarrow(mql.matches)
    mql.addEventListener('change', onChange)
    return () => mql.removeEventListener('change', onChange)
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setIsSubmitting(true)
    setError(null)
    try {
      await login(email, password)
    } catch {
      setError('Incorrect email or password.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="dpm-login">
      <div className="dpm-login__head">
        <div className="dpm-login__mark">
          <svg width="26" height="26" viewBox="0 0 34 34" aria-hidden="true">
            <circle cx="17" cy="17" r="15" className="ls" />
            <path d="M11 20.6l4-5.4 3.6 3 4.4-6.4" className="acc" />
          </svg>
          <div className="dpm-login__mark-bar" />
          <div>
            <strong>DPM</strong>
            <span>PERFORMANCE MONITORING</span>
          </div>
        </div>
        <button
          type="button"
          className="dpm-login__theme-btn"
          onClick={toggleTheme}
          aria-label="Switch colour scheme"
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 12.8A9 9 0 1111.2 3a7 7 0 009.8 9.8z" />
          </svg>
        </button>
      </div>

      <div className="dpm-login__grid">
        <LoginHero paused={signinActive || isNarrow} />

        <div
          className="dpm-login__signin"
          onMouseEnter={() => setSigninActive(true)}
          onMouseLeave={() => setSigninActive(false)}
          onFocus={() => setSigninActive(true)}
          onBlur={() => setSigninActive(false)}
        >
          <span className="dpm-login__eyebrow">SECURE ACCESS</span>
          <h4>Welcome back</h4>
          <form onSubmit={handleSubmit}>
            <div className="mb-3.5 space-y-1.5">
              <Label htmlFor="login-email" className="dpm-login__field-label">
                Email
              </Label>
              <Input
                id="login-email"
                type="email"
                autoComplete="username"
                placeholder="you@strides.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="dpm-login__input"
              />
            </div>
            <div className="mb-3.5 space-y-1.5">
              <Label htmlFor="login-password" className="dpm-login__field-label">
                Password
              </Label>
              <Input
                id="login-password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="dpm-login__input"
              />
            </div>
            {error && (
              <p role="alert" className="dpm-login__error mb-2">
                {error}
              </p>
            )}
            <Button type="submit" disabled={isSubmitting} className="dpm-login__submit">
              {isSubmitting ? 'Signing in…' : 'Enter DPM'}
            </Button>
          </form>
          <p className="dpm-login__fine">SSO · KRSG DOMAIN · 2026 STRIDES</p>
        </div>
      </div>
    </div>
  )
}
