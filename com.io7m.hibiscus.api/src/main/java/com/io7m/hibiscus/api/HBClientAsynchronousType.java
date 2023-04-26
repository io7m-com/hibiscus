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
import java.util.function.Function;

/**
 * The type of asynchronous RPC clients.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <R>  The type of responses returned from the server
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

public interface HBClientAsynchronousType<
  X extends Exception,
  C extends HBCommandType,
  R extends HBResponseType,
  RS extends R,
  RF extends R,
  E extends HBEventType,
  CR extends HBCredentialsType>
  extends HBClientStatusType<E>,
  HBClientCloseableType<X>
{
  /**
   * Poll the server for events. The events will be delivered via the
   * {@link HBClientStatusType#events()} observable stream.
   *
   * @return The operating in progress
   */

  CompletableFuture<Void> pollEvents();

  /**
   * Log in asynchronously.
   *
   * @param credentials The credentials
   *
   * @return The operation in progress.
   */

  CompletableFuture<HBResultType<RS, RF>> loginAsync(
    CR credentials);

  /**
   * Log in asynchronously. The result of the operation is mapped to an
   * exception if the command results in a failure response.
   *
   * @param credentials The credentials
   * @param exceptions  An exception-producing function.
   *
   * @return The result
   */

  @SuppressWarnings("unchecked")
  default CompletableFuture<RS>
  loginAsyncOrElseThrow(
    final CR credentials,
    final Function<RF, X> exceptions)
  {
    return this.loginAsync(credentials)
      .thenCompose(r -> {
        if (r instanceof HBResultFailure<RS, RF> failure) {
          return CompletableFuture.failedFuture(exceptions.apply(failure.result()));
        } else if (r instanceof HBResultSuccess<RS, RF> success) {
          return CompletableFuture.completedFuture(success.result());
        } else {
          throw new IllegalStateException();
        }
      });
  }

  /**
   * Execute the given command asynchronously.
   *
   * @param command The command
   *
   * @return The operation in progress.
   */

  CompletableFuture<HBResultType<RS, RF>> executeAsync(C command);

  /**
   * Execute the given command asynchronously. The result of the operation is
   * mapped to an exception if the command results in a failure response.
   *
   * @param command    The command
   * @param exceptions An exception-producing function.
   *
   * @return The result
   */

  default CompletableFuture<RS> executeAsyncOrElseThrow(
    final C command,
    final Function<RF, X> exceptions)
  {
    return this.executeAsync(command)
      .thenCompose(r -> {
        if (r instanceof HBResultFailure<RS, RF> failure) {
          return CompletableFuture.failedFuture(exceptions.apply(failure.result()));
        } else if (r instanceof HBResultSuccess<RS, RF> success) {
          return CompletableFuture.completedFuture(success.result());
        } else {
          throw new IllegalStateException();
        }
      });
  }

  /**
   * Disconnect the client.
   *
   * @return The operation in progress.
   */

  CompletableFuture<Void> disconnectAsync();
}
