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

import java.util.function.Function;

/**
 * The type of synchronous RPC clients.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <R>  The type of responses returned from the server
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

public interface HBClientSynchronousType<
  X extends Exception,
  C extends HBCommandType,
  R extends HBResponseType,
  RS extends R,
  RF extends R,
  E extends HBEventType,
  CR extends HBCredentialsType>
  extends HBClientSynchronousOperationsType<X, C, R, RS, RF, CR>,
  HBClientStatusType<C, R, RF, E, CR>,
  HBClientCloseableType<X>
{
  /**
   * Log in synchronously, or throw an exception built by {@code exceptions}
   * based on the failure response.
   *
   * @param credentials The credentials
   * @param exceptions  An exception-producing function.
   *
   * @return The result
   *
   * @throws X                    On errors
   * @throws InterruptedException On interruption
   */

  default RS loginOrElseThrow(
    final CR credentials,
    final Function<RF, X> exceptions)
    throws X, InterruptedException
  {
    return this.login(credentials).orElseThrow(exceptions);
  }

  /**
   * Execute the given command synchronously, or throw an exception built by
   * {@code exceptions} based on the failure response.
   *
   * @param command    The command
   * @param exceptions An exception-producing function.
   *
   * @return The result
   *
   * @throws X                    If command execution fails
   * @throws InterruptedException On interruption
   */

  default RS executeOrElseThrow(
    final C command,
    final Function<RF, X> exceptions)
    throws X, InterruptedException
  {
    return this.execute(command).orElseThrow(exceptions);
  }
}
