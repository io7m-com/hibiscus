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
import java.util.Optional;

/**
 * The type of transports for reading and writing messages.
 *
 * @param <M> The type of messages
 * @param <X> the type of exceptions
 */

public interface HBTransportType<
  M extends HBMessageType,
  X extends Exception>
  extends HBClientCloseableType<X>
{
  /**
   * Take a message from the transport, if one is available.
   *
   * @param timeout The timeout value
   *
   * @return The message
   *
   * @throws X On errors
   */

  Optional<M> read(
    Duration timeout)
    throws X;

  /**
   * Place a message on the transport.
   *
   * @param message The message
   *
   * @throws X On errors
   */

  void write(M message)
    throws X;
}
