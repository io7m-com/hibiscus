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

import java.util.Objects;

/**
 * A connection attempt failed with a message.
 *
 * @param message The message
 * @param <M>     The type of messages
 * @param <P>     The type of connection parameters
 * @param <R>     The type of extra result values
 * @param <X>     the type of exceptions
 */

public record HBConnectionFailed<
  M extends HBMessageType,
  P extends HBConnectionParametersType,
  R,
  X extends Exception>(
  M message)
  implements HBConnectionResultType<M, P, R, X>
{
  /**
   * A connection attempt failed with a message.
   *
   * @param message The message
   */

  public HBConnectionFailed
  {
    Objects.requireNonNull(message, "message");
  }
}
