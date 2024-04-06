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


package com.io7m.hibiscus.basic;

import com.io7m.hibiscus.api.HBConnectionParametersType;
import com.io7m.hibiscus.api.HBMessageType;

import java.util.Objects;

/**
 * A new client handler along with the message from the server.
 *
 * @param newHandler The new handler
 * @param message    The message
 * @param <M>        The type of messages
 * @param <P>        The type of connection parameters
 * @param <X>        the type of exceptions
 */

public record HBClientHandlerAndMessage<
  M extends HBMessageType,
  P extends HBConnectionParametersType,
  X extends Exception>(
  HBClientHandlerType<M, P, X> newHandler,
  M message)
{
  /**
   * A new client handler along with the message from the server.
   *
   * @param newHandler The new handler
   * @param message    The message
   */

  public HBClientHandlerAndMessage
  {
    Objects.requireNonNull(newHandler, "newHandler");
    Objects.requireNonNull(message, "message");
  }
}
