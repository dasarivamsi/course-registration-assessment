# Design Specification

## Scope

The application owns registration state for one configured course offering. It
supports reading and updating that offering, enrolling eligible learners,
waitlisting eligible learners when full, dropping enrolled learners, and FIFO
promotion.

Course creation, catalogs, identity integrations, payments, multiple terms,
durable persistence, and deployment are outside scope.

## Architecture

The implementation uses a conventional Spring package structure with
SOLID-oriented boundaries:

- `controller` contains versioned REST controllers.
- `service` contains use-case interfaces, orchestration, concurrency, and the
  eligibility strategy.
- `repository` contains the registration repository interface and in-memory
  implementation.
- `model` contains immutable domain records.
- `dto` contains immutable request and response contracts.
- `exception` contains domain errors and global HTTP exception mapping.
- `config` contains startup offering configuration.
- `RegistrationService` implements the use cases and owns orchestration,
  invariants, and concurrency.
- `EligibilityPolicy` is a strategy implemented by
  `AllPrerequisitesEligibilityPolicy`.
- `RegistrationRepository` is the single persistence port.
- `InMemoryRegistrationRepository` is its process-local adapter.

See `docs/CLASS_DIAGRAM.md` for class relationships.

## Domain and eligibility

`CourseOffering` and `Learner` are immutable Java records. All collection values
are defensively copied.

A learner is eligible when their completed course IDs contain every current
prerequisite ID. No prerequisites means every valid learner request is eligible.
Eligibility is checked before enrollment or waitlisting and rechecked before
promotion.

Completed courses are supplied on the enrollment request and retained as an
immutable learner snapshot because transcript and identity integrations are out
of scope.

## Data structures

- `HashMap<String, CourseOffering>` for offering lookup.
- `HashMap<String, Learner>` for learner lookup.
- `Map<String, Set<String>>` with `HashSet` values for unique enrollments.
- `Map<String, ArrayDeque<String>>` for FIFO waitlists.
- `Map<String, Set<String>>` for constant-time waitlist duplicate checks.

The waitlist deque and membership set are always changed together.

## Invariants

1. Enrollment count never exceeds capacity.
2. A learner appears no more than once in enrollment or the waitlist.
3. A learner cannot be enrolled and waitlisted simultaneously.
4. The waitlist deque and membership index contain the same learners.
5. Invalid and duplicate operations leave state unchanged.

## Promotion policy

When a drop or capacity increase opens a seat, promotion polls from the FIFO
head. A learner missing from storage or no longer satisfying the latest
prerequisites is permanently skipped. Scanning continues until capacity is full
or the queue is empty.

Prerequisite updates do not remove existing enrolled learners and do not eagerly
purge the waitlist.

## Concurrency

`RegistrationService` owns one fair `ReentrantLock`. Startup initialization,
reads, enrollment, drop and promotion, and course updates run under that same
lock. Compound checks and mutations therefore execute atomically across the
plain maps, sets, and deques.

Every acquisition uses `try/finally`. API callers receive immutable snapshots,
never internal collections.

## Logging

Application code uses the SLF4J API with Log4j2 as the runtime implementation.
`log4j2-spring.xml` configures console output.

The service records offering initialization and updates, enrollment and
waitlisting outcomes, drops, promotions, and skipped queue entries. The global
exception handler records rejected requests and status codes. Completed-course
and prerequisite collections are not written to logs.

## HTTP contract

All endpoints use `/api/v1`:

- `GET /api/v1/offerings/{offeringId}`
- `POST /api/v1/offerings/{offeringId}/enrollments`
- `DELETE /api/v1/offerings/{offeringId}/enrollments/{learnerId}`
- `PUT /api/v1/admin/offerings/{offeringId}`

There is no course-creation endpoint. The default offering is loaded from
configuration during startup.

Direct enrollment returns `201 Created`; completed synchronous waitlisting
returns `200 OK`. Missing offerings, learners, and enrollment resources return
`404 Not Found`. Duplicate state and invalid capacity return `409 Conflict`,
while prerequisite failure returns `422 Unprocessable Content`.
