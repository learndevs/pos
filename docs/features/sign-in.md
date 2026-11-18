# Feature: Sign-in

## Why
Every action is tied to a user and role; cashiers sign in fast with a PIN on a touch screen.

## Flow
1. App starts: `GET /api/setup/status`. No users yet -> **Create owner** form (name, username, PIN twice) -> owner created and signed in.
2. Otherwise **Sign in**: tap a user tile (`GET /api/auth/users`), enter PIN on the pad, `POST /api/auth/login` -> session token kept in memory.
3. Sign out (header or SIGN_OUT action): `POST /api/auth/logout`.

## Rules
- PIN 4-8 digits, BCrypt hashed. Wrong PIN and unknown user give the same message.
- 5 wrong PINs lock the user for 5 minutes (423). All attempts are audited.
- Sessions live in server memory, 12 h idle timeout; a server restart signs everyone out.

## Data
`app_user`, `role`, `role_permission`, `audit_log` (V1, V2 seed roles/permissions).

## API
| Method | Path | Permission |
|---|---|---|
| GET | `/api/setup/status` | public |
| POST | `/api/setup/owner` | public, only while no users exist |
| GET | `/api/auth/users` | public |
| POST | `/api/auth/login` | public |
| GET | `/api/auth/me` | session |
| POST | `/api/auth/logout` | session |

## UI
`features/users/`: `CreateOwner`, `SignIn`, `PinPad`; session in `app/authStore`.

## Tests
`identity/AuthFlowTest`, `identity/LaunchTokenTest`.
