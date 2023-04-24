hibiscus
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.hibiscus/com.io7m.hibiscus.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.hibiscus%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.io7m.hibiscus/com.io7m.hibiscus.svg?style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/hibiscus/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m/hibiscus.svg?style=flat-square)](https://codecov.io/gh/io7m/hibiscus)

![hibiscus](./src/site/resources/hibiscus.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m/hibiscus/main.linux.temurin.current.yml)](https://github.com/io7m/hibiscus/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m/hibiscus/main.linux.temurin.lts.yml)](https://github.com/io7m/hibiscus/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m/hibiscus/main.windows.temurin.current.yml)](https://github.com/io7m/hibiscus/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m/hibiscus/main.windows.temurin.lts.yml)](https://github.com/io7m/hibiscus/actions?query=workflow%3Amain.windows.temurin.lts)|

## Hibiscus

An API specification for simple RPC clients.

## Motivation

Many [io7m](https://www.io7m.com/) projects implement some form of RPC client.
The Hibiscus API is an attempt to define some common interface types so that
unrelated projects nevertheless end up with a consistent look and feel across
client implementations.

## Architecture

![architecture](./src/site/resources/arch.png?raw=true)

1. A `Configuration` is passed to a `ClientFactory` to produce a `Client`.
2. The `Client` sends `Credentials` to the server to log in.
3. The `Server` sends back `Responses` indicating if the login succeeded or
   failed.
4. Assuming success, the `Client` sends `Commands` to the server to perform
   operations.
5. The `Server` sends `Responses` to `Commands`.
6. The `Server` optionally publishes `Events` that can be observed by
   the `Client`.

A `Response` is encapsulated in a `Result` type that statically indicates
success or failure. Convenience methods are provided to transform `Result`
values into exceptions, and the `Result` type is both a functor and a monad.

