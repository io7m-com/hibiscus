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

import java.util.Objects;
import java.util.Optional;

/**
 * The client state.
 */

public sealed interface HBStateType
{
  /**
   * @return {@code true} if this state implies the client is either closing or
   * has closed
   */

  boolean isClosingOrClosed();

  /**
   * The client is connected.
   */

  record HBStateConnected()
    implements HBStateType
  {
    @Override
    public String toString()
    {
      return "CONNECTED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client is disconnected.
   */

  record HBStateDisconnected()
    implements HBStateType
  {
    @Override
    public String toString()
    {
      return "DISCONNECTED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client is authenticating with the server.
   *
   * @param credentials The credentials
   */

  record HBStateExecutingLogin(HBConnectionParametersType credentials)
    implements HBStateType
  {
    /**
     * The client is authenticating with the server.
     */

    public HBStateExecutingLogin
    {
      Objects.requireNonNull(credentials, "credentials");
    }

    @Override
    public String toString()
    {
      return "CONNECTING";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client successfully authenticated with the server.
   *
   * @param response The response
   */

  record HBStateConnectionSucceeded(HBMessageType response)
    implements HBStateType
  {
    /**
     * The client successfully authenticated with the server.
     */

    public HBStateConnectionSucceeded
    {
      Objects.requireNonNull(response, "response");
    }

    @Override
    public String toString()
    {
      return "CONNECTION_SUCCEEDED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client failed to connect to the server.
   *
   * @param exception The exception, if any
   * @param response The response, if any
   */

  record HBStateConnectionFailed(
    Optional<Exception> exception,
    Optional<HBMessageType> response)
    implements HBStateType
  {
    /**
     * The client failed to authenticate with the server.
     */

    public HBStateConnectionFailed
    {
      Objects.requireNonNull(response, "response");
      Objects.requireNonNull(exception, "exception");
    }

    @Override
    public String toString()
    {
      return "CONNECTION_FAILED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client has been instructed to close.
   */

  record HBStateClosing()
    implements HBStateType
  {
    @Override
    public String toString()
    {
      return "CLOSING";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return true;
    }
  }

  /**
   * The client has been closed.
   */

  record HBStateClosed()
    implements HBStateType
  {
    @Override
    public String toString()
    {
      return "CLOSED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return true;
    }
  }
}
