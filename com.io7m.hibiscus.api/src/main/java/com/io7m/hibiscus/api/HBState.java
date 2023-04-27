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
 * The client state.
 */

public enum HBState
{
  /**
   * The client is authenticating with the server.
   */

  CLIENT_EXECUTING_LOGIN {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client failed to authenticate with the server.
   */

  CLIENT_EXECUTING_LOGIN_FAILED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client successfully authenticated with the server.
   */

  CLIENT_EXECUTING_LOGIN_SUCCEEDED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client is connected.
   */

  CLIENT_CONNECTED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client is disconnected.
   */

  CLIENT_DISCONNECTED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client is executing a command.
   */

  CLIENT_EXECUTING_COMMAND {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client failed to execute a command.
   */

  CLIENT_EXECUTING_COMMAND_FAILED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client successfully executed a command.
   */

  CLIENT_EXECUTING_COMMAND_SUCCEEDED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client is polling the server for events.
   */

  CLIENT_POLLING_EVENTS {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client failed to poll the server for events.
   */

  CLIENT_POLLING_EVENTS_FAILED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client successfully polled the server for events.
   */

  CLIENT_POLLING_EVENTS_SUCCEEDED {
    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  },

  /**
   * The client has been instructed to close.
   */

  CLIENT_CLOSING {
    @Override
    public boolean isClosingOrClosed()
    {
      return true;
    }
  },

  /**
   * The client has been closed.
   */

  CLIENT_CLOSED {
    @Override
    public boolean isClosingOrClosed()
    {
      return true;
    }
  };

  /**
   * @return {@code true} if this state implies the client is either closing or
   * has closed
   */

  public abstract boolean isClosingOrClosed();
}
