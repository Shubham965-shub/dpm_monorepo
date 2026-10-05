import { z } from 'zod'

// Mirrors backend com.strides.dto.AuthDtos.UserPublic - camelCase, Jackson serializes as-is.
export const userPublicSchema = z.object({
  id: z.string(),
  email: z.string(),
  fullName: z.string(),
  displayName: z.string().nullable(),
  title: z.string().nullable(),
  department: z.string().nullable(),
  isActive: z.boolean(),
  // Fixed backend enum: 'CXO' | 'HOD' | 'MANAGER' | 'SUPERVISOR' | 'ANALYST' | 'ADMIN'. A user can
  // hold more than one. Only 'ADMIN' gates anything server-side today.
  roles: z.array(z.string()),
})
export type UserPublic = z.infer<typeof userPublicSchema>

// No cookie - both tokens travel in the response body (see backend AuthController) and the
// client persists them itself (session.ts, localStorage).
export const loginResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  tokenType: z.string(),
  user: userPublicSchema,
})

export const refreshResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
})

// Mirrors backend com.strides.dto.AuthDtos.TeamMember - GET /api/auth/me/analysts.
export const teamMemberSchema = z.object({
  id: z.string(),
  fullName: z.string(),
  title: z.string().nullable(),
})
export const teamMemberListSchema = z.array(teamMemberSchema)
export type TeamMember = z.infer<typeof teamMemberSchema>

// Mirrors backend com.strides.dto.AuthDtos.FeedbackTokenResponse - GET /api/auth/feedback-token.
export const feedbackTokenResponseSchema = z.object({
  token: z.string(),
})
