export class AuthError extends Error {
  public readonly code: string
  public readonly statusCode: number

  constructor(message: string, code: string, statusCode = 401) {
    super(message)
    this.name = "AuthError"
    this.code = code
    this.statusCode = statusCode
  }
}

export class TokenExpiredError extends AuthError {
  constructor() {
    super("Token has expired", "TOKEN_EXPIRED", 401)
  }
}

export class TokenInvalidError extends AuthError {
  constructor() {
    super("Invalid token", "TOKEN_INVALID", 401)
  }
}

export class TokenRevokedError extends AuthError {
  constructor() {
    super("Token has been revoked", "TOKEN_REVOKED", 401)
  }
}

export class RefreshTokenNotFoundError extends AuthError {
  constructor() {
    super("Refresh token not found", "REFRESH_TOKEN_NOT_FOUND", 401)
  }
}

export class InvalidCredentialsError extends AuthError {
  constructor() {
    super("Invalid email or password", "INVALID_CREDENTIALS", 401)
  }
}

export class UserNotFoundError extends AuthError {
  constructor() {
    super("User not found", "USER_NOT_FOUND", 404)
  }
}

export class UserAlreadyExistsError extends AuthError {
  constructor() {
    super("User already exists", "USER_ALREADY_EXISTS", 409)
  }
}

export function handleAuthError(error: unknown) {
  if (error instanceof AuthError) {
    return {
      error: {
        message: error.message,
        code: error.code,
      },
      status: error.statusCode,
    }
  }

  // Handle JWT-specific errors
  if (error instanceof Error) {
    if (error.message.includes("expired")) {
      return {
        error: {
          message: "Token has expired",
          code: "TOKEN_EXPIRED",
        },
        status: 401,
      }
    }

    if (error.message.includes("signature")) {
      return {
        error: {
          message: "Invalid token signature",
          code: "TOKEN_INVALID",
        },
        status: 401,
      }
    }

    if (error.message.includes("revoked")) {
      return {
        error: {
          message: "Token has been revoked",
          code: "TOKEN_REVOKED",
        },
        status: 401,
      }
    }
  }

  // Generic error
  return {
    error: {
      message: "Internal server error",
      code: "INTERNAL_ERROR",
    },
    status: 500,
  }
}
