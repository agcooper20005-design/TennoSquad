# TennoSquad

TennoSquad is a Warframe squad-recruitment web application designed to make finding and organizing squads for activities such as Void Fissures and Arbitrations faster and more structured.

Instead of relying entirely on recruitment chat, players can create listings, specify what they are running, and allow other players to quickly retrieve the information needed to join them.

> TennoSquad is currently in Phase 1 testing.

## Live Test

TennoSquad is currently hosted at:

https://spiderspot.fun

The current deployment is intended for early testing and development. Features, APIs, and stored data may change during this phase.

## Current Features

### Accounts & Authentication

- User account creation
- Secure BCrypt password hashing
- Session-based authentication
- Login and logout
- CSRF protection
- User and administrator roles
- Ownership-based authorization for listings

### Squad Listings

Players can create and manage recruitment listings for:

- Void Fissures
- Arbitrations

Listings support:

- OPEN, FULL, and CLOSED states
- Player counts
- Maximum squad sizes
- Host ownership
- Mission assignment
- Relic selection
- Relic refinement
- Expected activity duration
- Generated Warframe whisper commands

Listings automatically transition between OPEN and FULL as their player count changes.

### Void Fissures

Fissure listings can include:

- Current Warframe missions
- Relics
- Relic era compatibility validation
- Refinement level
- Mission expiration validation

Expired or inactive missions cannot be joined.

### Warframe Data Integration

TennoSquad integrates external Warframe data for current mission information.

The backend supports synchronization and maintenance of:

- Active Void Fissures
- Arbitrations
- Mission expiration
- Prime Resurgence relic data

Maintenance operations are restricted to administrators.

## Security

TennoSquad currently implements:

- Spring Security
- BCrypt password hashing
- HTTP session authentication
- CSRF/XSRF protection
- Role-based authorization
- USER and ADMIN roles
- Resource ownership validation
- Restricted administrative and maintenance APIs
- Separate public and private user DTOs

Additional edge-level abuse protection and rate limiting are planned through Cloudflare as deployment progresses.

## Tech Stack

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven

### Frontend

- React
- TypeScript
- Vite

### Infrastructure

- Cloudflare
- PostgreSQL
- Spring Boot embedded frontend hosting

## Architecture

TennoSquad uses a Spring Boot REST backend with a React frontend.

During development:

Frontend:
http://localhost:5173

Backend:
http://localhost:8093

For production, the React application is built with Vite and copied into Spring Boot's static resources. The packaged application serves both the frontend and REST API from the same origin.

## Testing

TennoSquad includes an automated end-to-end backend workflow runner.

The current workflow exercises:

- Account creation
- Authentication
- Multiple independent user sessions
- CSRF protection
- Authorization boundaries
- Listing ownership
- Fissure listing lifecycle
- Arbitration listing lifecycle
- Player-count transitions
- Mission assignment
- Relic/mission compatibility
- Duplicate listing prevention
- Logout/session invalidation
- Administrative endpoint restrictions
- Invalid request handling

Current regression result:

**134 / 134 tests passing**

## Project Status

### Phase 1 — Early Testing

TennoSquad is now deployed for initial real-world testing.

Current priorities include:

- Finding workflow and UI issues
- Testing authentication and session behavior
- Testing squad recruitment with multiple users
- Monitoring mission/relic synchronization
- Hardening deployment security
- Adding Cloudflare rate limiting
- Improving the frontend based on tester feedback

## Disclaimer

TennoSquad is an independent community project and is not affiliated with, endorsed by, or sponsored by Digital Extremes.

Warframe and all related names and trademarks are property of their respective owners.
