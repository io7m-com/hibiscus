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

