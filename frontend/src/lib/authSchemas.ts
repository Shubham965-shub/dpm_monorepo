import { z } from 'zod'

// Mirrors backend com.strides.dto.AuthDtos.UserPublic - camelCase, Jackson serializes as-is.
export const userPublicSchema = z.object({
  id: z.string(),
  email: z.string(),
  fullName: z.string(),
  displayName: z.string().nullable(),
  title: z.string().nullable(),
  // Fixed backend enum: 'QC' | 'QA' | 'MFG' | 'ENGG' | 'SCP'. A user can hold more than one (e.g.
  // the one CXO shared across Mfg/Engg/SC&P) - gates which dashboard(s) are shown (see
  // roleAccess.ts's hasDepartmentAccess), mirroring RequiresDepartment's server-side check. ADMIN
  // (the role) bypasses this regardless of departments.

  departments: z.array(z.string()).optional().default([]),
  isActive: z.boolean(),
  // Fixed backend enum: 'CXO' | 'HOD' | 'MANAGER' | 'SUPERVISOR' | 'ANALYST' | 'ADMIN' |
  // 'CLUSTER_HEAD'. A user can hold more than one. Only 'ADMIN' gates anything server-side today
  // (department-gated endpoints also let ADMIN through regardless of department).
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
