/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.hibiscus.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The base type of RPC client.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <E>  The type of events returned by the server
 * @param <CR> The type of credentials
 */

public interface HBClientType<
  X extends Exception,
  C extends HBCommandType,
  RS extends HBResponseType,
  RF extends HBResponseType,
  E extends HBEventType,
  CR extends HBCredentialsType>
  extends AutoCloseable
{
  /**
   * @return {@code true} if the client is connected
   */

  boolean isConnected();

  /**
   * @return A stream of events received from the server
   */

  Flow.Publisher<E> events();

  /**
   * @return A stream of state updates for the client
   */

  Flow.Publisher<HBState> state();

  /**
   * @return The value of {@link #state()} right now
   */

  HBState stateNow();

  /**
   * Log in synchronously.
   *
   * @param credentials The credentials
   * @param <RS1>       The response type indicating success
   *
   * @return The result
   */

  <RS1 extends RS>
  HBResultType<RS1, RF> login(CR credentials);

  /**
   * Log in synchronously, or throw an exception built by {@code exceptions}
   * based on the failure response.
   *
   * @param credentials The credentials
   * @param exceptions  An exception-producing function.
   * @param <RS1>       The response type indicating success
   *
   * @return The result
   *
   * @throws X On errors
   */

  @SuppressWarnings("unchecked")
  default <RS1 extends RS> RS1 loginOrElseThrow(
    final CR credentials,
    final Function<RF, X> exceptions)
    throws X
  {
    return (RS1) this.login(credentials).orElseThrow(exceptions);
  }

  /**
   * Execute the given command synchronously.
   *
   * @param command The command
   * @param <RS1>   The response type indicating success
   * @param <C1>    The command type
   *
   * @return The result
   */

  <C1 extends C, RS1 extends RS>
  HBResultType<RS1, RF> execute(C1 command);

  /**
   * Execute the given command synchronously, or throw an exception built by
   * {@code exceptions} based on the failure response.
   *
   * @param command    The command
   * @param exceptions An exception-producing function.
   * @param <RS1>      The response type indicating success
   * @param <C1>       The command type
   *
   * @return The result
   *
   * @throws X If command execution fails
   */

  @SuppressWarnings("unchecked")
  default <C1 extends C, RS1 extends RS> RS1
  executeOrElseThrow(
    final C1 command,
    final Function<RF, X> exceptions)
    throws X
  {
    return (RS1) this.execute(command).orElseThrow(exceptions);
  }

  /**
   * Execute a function asynchronously as the same thread as other client
   * operations.
   *
   * @param f   The function
   * @param <T> The type of results
   *
   * @return The result of the function
   */

  <T> CompletableFuture<T> runAsync(Supplier<T> f);

  /**
   * Log in asynchronously.
   *
   * @param credentials The credentials
   * @param <RS1>       The response type indicating success
   *
   * @return The operation in progress.
   */

  default <RS1 extends RS> CompletableFuture<HBResultType<RS1, RF>> loginAsync(
    final CR credentials)
  {
    return this.runAsync(() -> this.login(credentials));
  }

  /**
   * Log in asynchronously. The result of the operation is mapped to an
   * exception if the command results in a failure response.
   *
   * @param credentials The credentials
   * @param exceptions  An exception-producing function.
   * @param <RS1>       The response type indicating success
   * @param <C1>        The command type
   *
   * @return The result
   */

  @SuppressWarnings("unchecked")
  default <C1 extends C, RS1 extends RS> CompletableFuture<RS1>
  loginAsyncOrElseThrow(
    final CR credentials,
    final Function<RF, X> exceptions)
  {
    return this.loginAsync(credentials)
      .thenCompose(r -> {
        if (r instanceof HBResultFailure<RS, RF> failure) {
          return CompletableFuture.failedFuture(exceptions.apply(failure.result()));
        } else if (r instanceof HBResultSuccess<RS, RF> success) {
          return CompletableFuture.completedFuture((RS1) success.result());
        } else {
          throw new IllegalStateException();
        }
      });
  }

  /**
   * Execute the given command asynchronously.
   *
   * @param command The command
   * @param <RS1>   The response type indicating success
   * @param <C1>    The command type
   *
   * @return The operation in progress.
   */

  default <C1 extends C, RS1 extends RS>
  CompletableFuture<HBResultType<RS1, RF>> executeAsync(
    final C1 command)
  {
    return this.runAsync(() -> this.execute(command));
  }

  /**
   * Execute the given command asynchronously. The result of the operation is
   * mapped to an exception if the command results in a failure response.
   *
   * @param command    The command
   * @param exceptions An exception-producing function.
   * @param <RS1>      The response type indicating success
   * @param <C1>       The command type
   *
   * @return The result
   */

  @SuppressWarnings("unchecked")
  default <C1 extends C, RS1 extends RS> CompletableFuture<RS1>
  executeAsyncOrElseThrow(
    final C1 command,
    final Function<RF, X> exceptions)
  {
    return this.executeAsync(command)
      .thenCompose(r -> {
        if (r instanceof HBResultFailure<RS, RF> failure) {
          return CompletableFuture.failedFuture(exceptions.apply(failure.result()));
        } else if (r instanceof HBResultSuccess<RS, RF> success) {
          return CompletableFuture.completedFuture((RS1) success.result());
        } else {
          throw new IllegalStateException();
        }
      });
  }

  @Override
  void close()
    throws X;
}
