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

/**
 * The core operations exposed by synchronous clients. These operations are
 * application-specific and are the only parts that cannot be provided by a
 * generic implementation.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 */

public interface HBClientSynchronousOperationsType<
  X extends Exception,
  C extends HBCommandType,
  RS extends HBResponseType,
  RF extends HBResponseType,
  CR extends HBCredentialsType>
{
  /**
   * Poll the server for events. The events will be delivered via the
   * {@link HBClientStatusType#events()} observable stream.
   *
   * @throws InterruptedException On interruption
   */

  void pollEvents()
    throws InterruptedException;

  /**
   * Log in synchronously.
   *
   * @param credentials The credentials
   * @param <RS1>       The response type indicating success
   *
   * @return The result
   *
   * @throws InterruptedException On interruption
   */

  <RS1 extends RS>
  HBResultType<RS1, RF> login(CR credentials)
    throws InterruptedException;

  /**
   * Execute the given command synchronously.
   *
   * @param command The command
   * @param <RS1>   The response type indicating success
   * @param <C1>    The command type
   *
   * @return The result
   *
   * @throws InterruptedException On interruption
   */

  <C1 extends C, RS1 extends RS>
  HBResultType<RS1, RF> execute(C1 command)
    throws InterruptedException;

  /**
   * Disconnect the client.
   *
   * @throws X                    On errors
   * @throws InterruptedException On interruption
   */

  void disconnect()
    throws X, InterruptedException;
}
