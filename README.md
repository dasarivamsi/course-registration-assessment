# Course Waitlist Service

Java 21 and Spring Boot implementation of the bounded course-enrollment,
dropping, FIFO waitlist, and promotion workflow.

## Requirements

- Java 21
- Maven 3.9+

On Apple Silicon with Homebrew:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

## Run

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080` and loads the default
`CS201-FALL-2026` offering from `src/main/resources/application.yml`.
Console logs use SLF4J with the Log4j2 backend configured in
`src/main/resources/log4j2-spring.xml`.

## Test

```bash
mvn test
```

## API examples

Read the default offering:

```bash
curl http://localhost:8080/api/v1/offerings/CS201-FALL-2026
```

Enroll an eligible learner:

```bash
curl -i -X POST \
  http://localhost:8080/api/v1/offerings/CS201-FALL-2026/enrollments \
  -H 'Content-Type: application/json' \
  -d '{
    "learnerId": "learner-1",
    "completedCourseIds": ["CS101", "MATH101"]
  }'
```

Update the offering:

```bash
curl -i -X PUT \
  http://localhost:8080/api/v1/admin/offerings/CS201-FALL-2026 \
  -H 'Content-Type: application/json' \
  -d '{
    "courseId": "CS201",
    "capacity": 3,
    "prerequisiteCourseIds": ["CS101", "MATH101", "STAT101"]
  }'
```

Drop an enrolled learner:

```bash
curl -i -X DELETE \
  http://localhost:8080/api/v1/offerings/CS201-FALL-2026/enrollments/learner-1
```

Successful direct enrollment returns `201`; successful waitlisting returns
`200`. Errors contain `code`, `message`, and `timestamp`. Dropping a learner
who is not enrolled returns `404`.

## Project documentation

- `docs/CLASS_DIAGRAM.md`
- `DESIGN.md`
- `LIMITATIONS.md`
- `AI_USE_RECORD.md`
