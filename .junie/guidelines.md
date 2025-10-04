# Project Guidelines

Project overview
- Name: poli-chrono
- Description: Java application built with Quarkus 3.28.x. It uses CDI via quarkus-arc and includes 
- Build tool and Java: Maven with Java 21 (maven.compiler.release=21).
- Dev mode: ./mvnw quarkus:dev (Dev UI at http://localhost:8080/q/dev/ in dev mode).
- Packaging: ./mvnw package (produces target/quarkus-app/). Uber-jar: ./mvnw package -Dquarkus.package.jar.type=uber-jar.
