# AI Prompt and Use Record — Working Draft

This is a working record of AI-assisted engineering activity. Entries may be
condensed before final submission, but retained entries must accurately describe
the prompt, AI contribution, human decision, and verification.

## Entry 001 — Requirements extraction

**Prompt**

> Read the attached PDF and list out the business and functional requirements.
> Aim: deliver the executable project according to the requirements and
> deliverables mentioned in the document. We will go step by step.

**AI contribution**

- Extracted the business objective, functional behavior, required test coverage,
  deliverables, and explicit scope exclusions from the assessment.
- Identified enrollment, dropping, FIFO promotion, prerequisite enforcement,
  duplicate handling, and state integrity as the core behavior.

**Human review and disposition**

- Accepted as a requirements summary.
- No implementation decisions were made from this response alone.

**Verification**

- Compared the summary against both pages of
  `Vamsi Krishna Dasari_AI_Assessment.pdf`.

## Entry 002 — Waitlist eligibility clarification

**Prompt**

> For the ineligible learner at front, remove the ineligible learner from the
> front and promote the next eligible learner. Also confirm how we decide that
> the learner is ineligible; the document does not appear to specify this.

**AI contribution**

- Confirmed that the assessment requires a stated prerequisite rule but does not
  prescribe a concrete prerequisite, learner eligibility model, or data source.
- Proposed treating eligibility as satisfaction of the documented prerequisite
  at the time of both initial enrollment and waitlist promotion.

**Human review and disposition**

- Accepted the queue policy: during promotion, permanently remove ineligible
  learners encountered at the front and continue in FIFO order until an eligible
  learner is promoted or the waitlist is empty.
- Eligibility representation remains to be selected based on the starter project
  or chosen minimal implementation stack.

**Verification**

- The policy will be verified with focused tests for:
  - direct prerequisite failure;
  - an ineligible learner at the front of the waitlist;
  - promotion of the next eligible learner;
  - removal of the skipped ineligible learner;
  - an entirely ineligible waitlist leaving the seat vacant.

## Entry 003 — Eligibility model review

**Prompt**

> If eligibility is held as an in-memory set of learner IDs, how are those IDs
> known beforehand? An ineligible learner should not be added to the waitlist,
> so how can the ineligible-at-promotion scenario be tested? The proposed model
> may not be correct.

**AI contribution**

- Reassessed the earlier suggestion and identified that a bare set of eligible
  IDs does not explain the source of eligibility and is too weak as the domain
  model.
- Proposed deriving eligibility from the current learner record through a small
  prerequisite-checking boundary.
- Explained that eligibility must be checked before joining the waitlist and
  rechecked at promotion because learner data may be corrected or changed while
  the learner waits.

**Human review and disposition**

- Rejected the bare eligible-ID set as the implementation's domain model.
- Retained an in-memory learner repository as an acceptable fallback persistence
  mechanism when no starter project or database is supplied.
- Accepted in revised form: represent a learner by ID and completed course IDs,
  then derive eligibility by checking all prerequisites required by the offering.

**Planned verification**

- Create an enrolled learner so the offering is full.
- Add learner A, who currently satisfies the prerequisite, to the waitlist.
- Correct learner A's record so the prerequisite is no longer satisfied.
- Add eligible learner B behind learner A.
- Drop the enrolled learner.
- Assert that A is removed, B is promoted, and capacity remains correct.

## Entry 004 — Multiple-prerequisite rule

**Prompt**

> Each course can have multiple prerequisites. A learner must have completed all
> of them.

**AI contribution**

- Confirmed that an all-prerequisites rule is deterministic, testable, and
  compatible with the assessment's bounded scope.
- Recommended representing required and completed course IDs as sets and
  checking that the learner's completed set contains every required ID.

**Human review and disposition**

- Accepted as the prerequisite eligibility rule.
- No configurable policy engine or course-catalog feature will be introduced.

**Planned verification**

- A learner with all prerequisites is eligible.
- A learner missing one or more prerequisites is ineligible.
- A course with no prerequisites accepts any known learner.
- Eligibility is rechecked before promotion.

## Entry 005 — Prerequisite updates while learners wait

**Prompt**

> Course prerequisites can be updated at any time. Can this be used to test
> removing an ineligible learner from the waitlist?

**AI contribution**

- Confirmed that changing prerequisites after learners join the waitlist creates
  a realistic reason to recheck eligibility during promotion.
- Recommended explicit update semantics: existing enrollments remain unchanged,
  waitlisted learners are not eagerly purged, and each learner is checked against
  the latest prerequisites when considered for promotion.

**Human review and disposition**

- Accepted prerequisite changes as a source of eligibility changes.
- Kept the behavior bounded to one offering; no configurable policy engine or
  course-catalog feature is required.

**Planned verification**

- Fill the offering when its prerequisite set is `{P1}`.
- Waitlist learner A (completed `{P1}`) followed by learner B (completed
  `{P1, P2}`).
- Change the offering prerequisites to `{P1, P2}`.
- Drop an enrolled learner.
- Assert that A is removed as currently ineligible and B is promoted.
- Assert that prerequisite changes do not remove existing enrolled learners.

## Current decisions

1. One course offering only.
2. Waitlist order is FIFO.
3. Eligibility is checked both when a learner requests enrollment and when a
   learner is considered for promotion.
4. An ineligible learner encountered during promotion is removed from the
   waitlist, then the next learner is evaluated.
5. Ineligible learners are rejected before enrollment or waitlisting.
6. Eligibility is derived from current learner data rather than represented by
   a bare set of eligible learner IDs.
7. A course offering may require multiple prerequisite course IDs. A learner is
   eligible only when their completed-course IDs contain every required ID.
8. A course with no prerequisites treats every known learner as eligible.
9. Prerequisites may change while learners are waiting. Promotion uses the
   latest prerequisite set.
10. Prerequisite changes do not retroactively remove enrolled learners and do
    not immediately purge the waitlist.

## Entry 006 — API versioning, startup data, and collection design

**Prompts**

> Maintain API versioning, load a default course when the application starts,
> and explain the data structures and concurrency approach.

> Enrolled learner order is not required. Consider course and learner lookup
> maps, an enrollment map of sets, a waitlist map of FIFO deques, and a
> waitlist-membership map of sets. Update the plan with API versioning and
> remove the API for registering a new course while retaining course updates.

**AI contribution**

- Recommended URI-based major versioning beginning with `/api/v1`.
- Identified a conflict between preloading the only supported offering and
  exposing a course-creation API: once the default is loaded, creation cannot
  succeed without exceeding the one-offering boundary.
- Recommended loading the default offering from `application.yml`, removing
  course creation, and retaining the course-update API.
- Initially suggested `LinkedHashSet` for deterministic enrollment output, then
  accepted that ordering is not a requirement and `HashSet` better represents
  the domain.
- Reviewed the proposed lookup maps, enrollment sets, FIFO deques, and companion
  waitlist sets. Confirmed that they satisfy lookup, uniqueness, duplicate
  prevention, and FIFO requirements when updated under one lock.
- Clarified that map-level thread safety alone cannot make compound enrollment,
  drop, and promotion operations atomic.

**Human review and disposition**

- Accepted `/api/v1` as the initial API contract.
- Accepted startup loading of one default offering.
- Rejected a course-creation endpoint; retained only course update
  administration.
- Rejected ordered enrollment storage because enrolled learner order has no
  business meaning.
- Accepted `Set<String>` for enrollment uniqueness, `ArrayDeque<String>` for
  FIFO order, and a companion `Set<String>` for constant-time waitlist duplicate
  checks.
- Accepted one fair `ReentrantLock` around complete service operations, with
  immutable copies returned to API callers.

**Files and decisions informed**

- Updated `course_waitlist_design_5acc8027.plan.md` with versioned endpoints,
  startup default-offering behavior, and removal of course creation.
- Final implementation must keep enrollment and waitlist collections
  synchronized atomically and remove dequeued learners from the companion
  waitlist-membership set.

## Additional current decisions

11. All public endpoints use the `/api/v1` prefix.
12. Exactly one default offering is loaded from application configuration at
    startup.
13. There is no course-creation endpoint; the admin API can update the default
    offering.
14. Enrolled learner order is not significant, so enrollment uses an unordered
    set.
15. Waitlisting uses a FIFO deque plus a membership set for efficient duplicate
    checks.
16. Related collection reads and writes execute under the same lock; API
    responses contain immutable snapshots rather than internal collections.

## Entry 007 — Finalize data structures and concurrency plan

**Prompt**

> Update the design plan with the final chosen data structures and concurrency
> handling.

**AI contribution**

- Documented the exact maps, sets, and deque used for offering lookup, learner
  lookup, enrollment uniqueness, FIFO ordering, and waitlist duplicate checks.
- Documented the lock boundary, atomic workflows, immutable snapshot rule, and
  `try/finally` requirement.
- Removed `ConcurrentHashMap` from the design because the selected plain
  collections are private and every access is guarded by the same lock.

**Human review and disposition**

- Accepted the previously discussed data structures and fair global lock as the
  final implementation design for the bounded one-offering service.

**File informed**

- `course_waitlist_design_5acc8027.plan.md`

## Entry 008 — SOLID principles and design patterns

**Prompt**

> Adhere to appropriate design patterns and SOLID principles. Use interfaces
> where required and make POJO fields immutable.

**AI contribution**

- Recommended interfaces at meaningful application and persistence boundaries
  rather than creating an interface for every class.
- Applied ports-and-adapters layering, dependency inversion, interface
  segregation, Repository, and Strategy patterns to the design.
- Proposed separate registration and course-administration use-case interfaces,
  focused repository interfaces, and an `EligibilityPolicy` strategy.
- Kept the service as the single concurrency coordinator so one lock protects
  complete workflows across all repositories.
- Specified immutable Java records, final state, defensive collection copies,
  constructor validation, and replacement of course values during updates.

**Human review and disposition**

- Accepted SOLID and design-pattern adherence as implementation constraints.
- Accepted interfaces for use cases, repositories, and eligibility behavior.
- Retained concrete immutable domain records and DTOs because they contain data,
  not substitutable behavior.

**File informed**

- `course_waitlist_design_5acc8027.plan.md`

## Entry 009 — Consolidate repository interfaces

**Prompt**

> Keep the design simple by replacing the separate repository interfaces with
> one `RegistrationRepository` supporting offering and learner lookup,
> enrollment operations, FIFO queue operations, and snapshots.

**AI contribution**

- Confirmed that one repository is cohesive for this bounded registration slice
  and avoids unnecessary interface ceremony.
- Identified required additions to the proposed contract: saving immutable
  offering and learner values, checking waitlist membership, and reading
  enrollment count.
- Preserved encapsulation by keeping all maps, sets, and deques private in
  `InMemoryRegistrationRepository`.
- Kept the fair service-level lock as the atomic boundary across compound
  repository calls.

**Human review and disposition**

- Replaced separate course-offering, learner, and registration-state repository
  interfaces with one `RegistrationRepository`.
- Retained use-case and eligibility interfaces because they represent distinct
  behavioral boundaries.

**File informed**

- `course_waitlist_design_5acc8027.plan.md`

## Entry 010 — Save finalized class diagram

**Prompt**

> Save the class diagram with the latest changes in the project structure.

**AI contribution**

- Produced an implementation-level Mermaid class diagram reflecting the final
  use-case interfaces, consolidated `RegistrationRepository`, in-memory data
  structures, eligibility strategy, immutable records, startup initializer,
  error handling, API versioning, and service-level concurrency boundary.
- Added concise implementation constraints below the diagram.

**Human review and disposition**

- Saved as the current class-design reference for implementation.

**File informed**

- `docs/CLASS_DIAGRAM.md`

## Entry 011 — Implement finalized design

**Prompt**

> Build the code according to the finalized class diagram and design plan.

**AI contribution**

- Implemented the Java 21 Spring Boot service using versioned controllers,
  use-case interfaces, one consolidated `RegistrationRepository`, an in-memory
  adapter, an eligibility strategy, immutable records, and constructor
  injection.
- Implemented startup loading of the configured default offering with no
  course-creation endpoint.
- Implemented enrollment, duplicate prevention, FIFO waitlisting, dropping,
  current-prerequisite revalidation, skipping ineligible queue entries, and
  promotion after drops or capacity increases.
- Implemented one fair service-level lock around startup, reads, updates, and
  compound registration workflows.
- Added focused service, API, startup, validation, prerequisite-change, and
  concurrency tests plus setup, design, and limitations documentation.

**Human review and disposition**

- Implementation follows the approved design and remains ready for human review.

**Files informed**

- `src/main/java/com/assessment/coursewaitlist/`
- `src/main/resources/application.yml`
- `src/test/java/com/assessment/coursewaitlist/`
- `README.md`, `DESIGN.md`, and `LIMITATIONS.md`

**Verification evidence**

- Compiled and tested with Java 21 and Maven.
- Result: 17 tests run, 0 failures, 0 errors, 0 skipped.
- Covered direct enrollment, full capacity, duplicate requests, FIFO promotion,
  prerequisite failure and updates, skipped ineligible learners, invalid and
  repeated drops, startup configuration, API versioning, course updates,
  capacity invariants, and concurrent requests.

## Entry 012 — Conventional package structure and logging

**Prompt**

> Refactor the code to the recommended conventional Spring structure and add
> logging using a Log4j-like package.

**AI contribution**

- Refactored production code into `controller`, `service`, `repository`,
  `model`, `dto`, `exception`, and `config` packages.
- Moved the common error response to `dto` and renamed the advice to
  `GlobalExceptionHandler`, clarifying the distinction between response schema
  and exception-to-HTTP mapping.
- Used the SLF4J API with Log4j2 as the runtime backend, keeping source code
  independent of the logging implementation.
- Added Log4j2 console configuration and operational logs for initialization,
  enrollment, waitlisting, dropping, promotion, course updates, skipped
  learners, validation failures, and rejected domain requests.
- Avoided logging completed-course and prerequisite collections.

**Human review and disposition**

- Accepted the conventional package organization.
- Accepted SLF4J with Log4j2 rather than coupling classes directly to Log4j2.

**Files informed**

- `course_waitlist_design_5acc8027.plan.md`
- `pom.xml`
- `src/main/java/com/assessment/coursewaitlist/`
- `src/main/resources/log4j2-spring.xml`
- `docs/CLASS_DIAGRAM.md`, `DESIGN.md`, `README.md`, and `LIMITATIONS.md`

**Verification evidence**

- Ran the complete Java 21 Maven suite after the refactor.
- Result: 17 tests run, 0 failures, 0 errors, 0 skipped.
- Test output confirmed Log4j2-formatted service and exception logs.

## Entry 013 — Refine HTTP status semantics

**Prompt**

> Discuss the returned status codes and apply the recommended changes.

**AI contribution**

- Identified that `202 Accepted` implies unfinished asynchronous processing,
  while waitlist insertion completes synchronously.
- Recommended `200 OK` for a completed `WAITLISTED` outcome while retaining
  `201 Created` for direct enrollment.
- Recommended `404 Not Found` for `NOT_ENROLLED` because the targeted enrollment
  resource does not exist, while retaining a clear error response for repeated
  drops.

**Human review and disposition**

- Accepted both status-code changes.

**Files informed**

- `RegistrationController.java`
- `GlobalExceptionHandler.java`
- API tests, `README.md`, `DESIGN.md`, and the design plan

**Verification evidence**

- Ran the complete Java 21 Maven test suite with updated API assertions.
- Result: 17 tests run, 0 failures, 0 errors, 0 skipped.
- Confirmed `WAITLISTED` returns `200` and repeated drop returns `404` with
  `NOT_ENROLLED`.
