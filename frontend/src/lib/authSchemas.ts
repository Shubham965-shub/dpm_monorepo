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
  roles: z.array(z.string()),
  permissions: z.array(z.string()),
})
export type UserPublic = z.infer<typeof userPublicSchema>

export const loginResponseSchema = z.object({
  accessToken: z.string(),
  tokenType: z.string(),
  user: userPublicSchema,
})

export const refreshResponseSchema = z.object({
  accessToken: z.string(),
})
