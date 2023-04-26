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

package com.io7m.hibiscus.basic;

import com.io7m.hibiscus.api.HBCommandType;
import com.io7m.hibiscus.api.HBCredentialsType;
import com.io7m.hibiscus.api.HBEventType;
import com.io7m.hibiscus.api.HBResponseType;

import java.util.Objects;

/**
 * The result of negotiating a client handler.
 *
 * @param <X>           The type of exceptions that can be raised by the client
 * @param <C>           The type of commands sent by the client
 * @param <RS>          The type of responses returned that indicate successful
 *                      commands
 * @param <RF>          The type of responses returned that indicate failed
 *                      commands
 * @param <CR>          The type of credentials
 * @param <E>           The type of events
 * @param newHandler    The new handler
 * @param loginResponse The login response message
 */

public record HClientNewHandler<
  X extends Exception,
  C extends HBCommandType,
  RS extends HBResponseType,
  RF extends HBResponseType,
  E extends HBEventType,
  CR extends HBCredentialsType>(
  HClientHandlerType<X, C, RS, RF, E, CR> newHandler,
  RS loginResponse)
{
  /**
   * The result of negotiating a client handler.
   *
   * @param newHandler    The new handler
   * @param loginResponse The login response message
   */

  public HClientNewHandler
  {
    Objects.requireNonNull(newHandler, "newHandler");
    Objects.requireNonNull(loginResponse, "loginResponse");
  }
}
