# poli-chrono

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

Junie AI built this entirely.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/poli-chrono-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Windows installer (bundled Java runtime)

This project can build a Windows installer using JReleaser's jpackage assembler, executed via a Windows-specific Maven profile. It packages the application together with a private Java runtime (no system Java required).

Build the installer on Windows 10/11 with JDK 21 (Temurin recommended):

```powershell
# From the repository root
./mvnw -B -ntp -Dquarkus.package.jar.type=uber-jar -Pwindows-release package
```

What it does:
- Builds an uber-jar (Quarkus runner jar).
- Uses JReleaser + jpackage to produce a self-contained .exe installer that bundles a Java runtime.

Output:
- Installer EXE available under target/ (example: Poli Chrono-1.0-SNAPSHOT.exe or similar depending on version).
- The installed application includes its own Java and does not depend on system Java.

CI builds:
- GitHub Actions workflow .github/workflows/windows-installer.yml builds the installer on windows-latest and uploads it as an artifact named "windows-installer" containing target/*.exe.

## Data persistence

- Speakers and all app properties (auto-stop, title, UI sizes for admin and audience) are persisted to a JSON file.
- Default location: ./data/speakers.json (configurable via `speakers.file` in application.properties).
- Backward compatible: if an older file contains just an array of speakers, it will be loaded; saving will migrate to an object with properties + speakers.

Example structure:

```
{
  "version": 1,
  "autoStopOnStart": true,
  "title": "My Event",
  "uiCardWidth": 360,
  "uiTextScale": 100,
  "uiActionSize": 56,
  "uiCardWidthMain": 360,
  "uiTextScaleMain": 100,
  "speakers": [ { ... } ]
}
```

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
