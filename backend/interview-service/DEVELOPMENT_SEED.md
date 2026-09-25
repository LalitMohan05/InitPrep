# Development question seed

The seed is disabled by default. It runs only with the `dev` Spring profile and
`initprep.seed.enabled=true`.

From this directory, run:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=dev '-Dspring-boot.run.arguments=--initprep.seed.enabled=true --server.port=0'
```

Stop the process after the log reports that the development dataset was seeded.

This replaces all questions, test cases, and question-to-topic/company links in
the configured interview database. It preserves user/authentication records and
the topic/company catalog, reusing catalog entries by name. The seeder validates
the full dataset before deletion and verifies persisted counts and relationships
before committing. Do not run it against a non-development database.

The dataset contains 30 unique coding questions (10 per difficulty) and 30 test
cases per question (3 public, 27 hidden). Java starter code uses the Judge0
stdin/stdout entry point: `public class Main` with `public static void main`.
