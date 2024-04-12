/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * The I/O operations.
 *
 * @param <M> The type of messages
 * @param <X> the type of exceptions
 */

public interface HBIOOperationsType<
  M extends HBMessageType,
  X extends Exception>
{
  /**
   * Take a message from the transport, if one is available.
   *
   * @param timeout The timeout value
   *
   * @return The message
   *
   * @throws X                    On errors
   * @throws InterruptedException On interruption
   */

  HBReadType<M> receive(
    Duration timeout)
    throws X, InterruptedException;

  /**
   * Place a message on the transport. The caller is expected to manually
   * read back a response later.
   *
   * @param message The message
   *
   * @throws X                    On errors
   * @throws InterruptedException On interruption
   */

  void send(M message)
    throws X, InterruptedException;

  /**
   * Place a message on the transport. The transport is not required to track
   * the message for later response resolution; any response to this message
   * might be returned to the caller as an ordinary message.
   *
   * @param message The message
   *
   * @throws X                    On errors
   * @throws InterruptedException On interruption
   */

  void sendAndForget(M message)
    throws X, InterruptedException;

  /**
   * Place a message on the transport and wait for a response.
   *
   * @param message The message
   * @param timeout The timeout
   *
   * @return The response
   *
   * @throws X                    On errors
   * @throws InterruptedException On interruption
   * @throws TimeoutException     If no response is returned within the given timeout
   */

  M sendAndWait(
    M message,
    Duration timeout)
    throws X, InterruptedException, TimeoutException;
}
