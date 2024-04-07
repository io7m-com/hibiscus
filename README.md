hibiscus
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.hibiscus/com.io7m.hibiscus.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.hibiscus%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/com.io7m.hibiscus/com.io7m.hibiscus?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/hibiscus/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m/hibiscus.svg?style=flat-square)](https://codecov.io/gh/io7m/hibiscus)

![com.io7m.hibiscus](./src/site/resources/hibiscus.jpg?raw=true)

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
2. The `Client` sends `ConnectionParameters` to the server to log in.
3. The `Server` sends back `Messages` indicating if the login succeeded or
   failed.
4. Assuming success, the `Client` sends `Messages` to the server to perform
   operations.
5. The `Server` sends `Messages` in response to `Messages`.

## State

Clients conform to a simple state machine:

![state](./src/site/resources/state.png?raw=true)

1. Clients begin in the `DISCONNECTED` state.
2. A `Login` operation moves the client into the `CONNECTING` state.
3. If the login succeeds, the client moves to the `CONNECTED` state after
   passing through the `CONNECTION_SUCCEEDED` state.
4. If the login fails, the client moves to the `DISCONNECTED` state after
   passing through the `CONNECTION_FAILED` state.

In the `CONNECTED` state, the client can:

* Disconnect, moving to the `DISCONNECTED` state.
* Send messages, remaining in the `CONNECTED` state.

A client maintains an internal [Connection](#connection) that performs
the actual communication.

## Transport

A _Transport_ represents an object that can be used to read and write messages.
A _Transport_ exposes two operations; `read` and `write`. The `read` operation
attempts to read a message from the transport, waiting up to a configurable
timeout value. The `write` operation attempts to write a message to the
transport.

The package includes multiple example transports:

  * [EUDP0Transport](com.io7m.hibiscus.examples/src/main/java/com/io7m/hibiscus/examples/udp0/EUDP0Transport.java) - A UDP transport
  * [ETCP0Transport](com.io7m.hibiscus.examples/src/main/java/com/io7m/hibiscus/examples/tcp0/ETCP0Transport.java) - A TCP transport
  * [EHTTP0Transport](com.io7m.hibiscus.examples/src/main/java/com/io7m/hibiscus/examples/http0/EHTTP0Transport.java) - An HTTP transport

## Connection

A _Connection_ encapsulates a [Transport](#transport) and imposes
connection-like semantics upon it. A connection provides three operations:
`send`, `receive`, and `ask`.

The `send` operation delegates the underlying transport's `write` operation.

The `receive` operation maintains an internal receipt queue, but otherwise
delegates to the underlying transport's `read` operation.

The `ask` operation sends a message using the underlying transport's `write`
operation, and then waits until the internal receipt queue contains a message
that indicates that it is a response for the original message.

