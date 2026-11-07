# Limitations and Follow-up Risks

- All state is in memory and is lost when the process restarts.
- Multiple application instances would not share state.
- Exactly one configured offering is supported.
- Enrollment requests self-report completed courses; production should use a
  trusted transcript service.
- Admin endpoints are unauthenticated and intended only for this assessment.
- The global lock prioritizes correctness over throughput and is appropriate
  only for the bounded one-offering scope.
- There is no endpoint to cancel a waitlist entry.
- Prerequisite updates do not retroactively remove enrolled learners.
- Waitlist eligibility is checked lazily during promotion.
- State responses expose learner IDs for review; production requires privacy
  controls and authorization.
- Logs are console-only and have no durable sink, correlation ID, or centralized
  aggregation.
