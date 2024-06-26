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

import com.io7m.hibiscus.api.HBStateType.HBStateClosed;
import com.io7m.hibiscus.api.HBStateType.HBStateClosing;
import com.io7m.hibiscus.api.HBStateType.HBStateConnected;
import com.io7m.hibiscus.api.HBStateType.HBStateConnecting;
import com.io7m.hibiscus.api.HBStateType.HBStateConnectionFailed;
import com.io7m.hibiscus.api.HBStateType.HBStateConnectionSucceeded;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import net.jcip.annotations.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeoutException;

/**
 * An abstract client implementation.
 *
 * @param <M> The type of messages
 * @param <P> The type of connection parameters
 * @param <X> the type of exceptions
 */

public abstract class HBClientAbstract<
  M extends HBMessageType,
  P extends HBConnectionParametersType,
  X extends Exception>
  implements HBClientType<M, P, X>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(HBClientAbstract.class);

  private final Object stateLock;
  private final HBClientHandlerType<M, P, X> disconnectedHandler;
  private volatile HBClientHandlerType<M, P, X> handler;
  @GuardedBy("stateLock")
  private HBStateType stateNow;
  private final SubmissionPublisher<HBStateType> statePublisher;

  protected HBClientAbstract(
    final HBClientHandlerType<M, P, X> inHandler)
  {
    this.handler =
      Objects.requireNonNull(inHandler, "disconnectedHandler");
    this.disconnectedHandler =
      Objects.requireNonNull(inHandler, "disconnectedHandler");

    this.statePublisher =
      new SubmissionPublisher<>(
        new HBDirectExecutor(),
        Flow.defaultBufferSize());
    this.stateLock =
      new Object();
    this.stateNow =
      new HBStateDisconnected();
  }

  @Override
  public final HBStateType stateNow()
  {
    synchronized (this.stateLock) {
      return this.stateNow;
    }
  }

  protected final HBClientHandlerType<M, P, X> handler()
  {
    return this.handler;
  }

  @Override
  public final HBConnectionResultType<M, P, ?, X> connect(
    final P parameters)
    throws InterruptedException
  {
    Objects.requireNonNull(parameters, "parameters");

    this.checkNotClosingOrClosed();

    try {
      this.disconnect();
    } catch (final Exception e) {
      // Ignore
    }

    this.publishState(new HBStateConnecting(parameters));

    try {
      return switch (this.handler.doConnect(parameters)) {
        case final HBConnectionError<
          M, P, HBClientHandlerType<M, P, X>, X> error -> {
          this.publishState(new HBStateConnectionFailed(
            Optional.of(error.exception()),
            Optional.empty()
          ));
          LOG.debug("Login failed {}", error);
          yield new HBConnectionError<>(error.exception());
        }

        case final HBConnectionFailed<
          M, P, HBClientHandlerType<M, P, X>, X> failed -> {
          this.publishState(new HBStateConnectionFailed(
            Optional.empty(),
            Optional.of(failed.message())
          ));
          LOG.debug("Login failed {}", failed);
          yield new HBConnectionFailed<>(failed.message());
        }

        case final HBConnectionSucceeded<
          M, P, HBClientHandlerType<M, P, X>, X> succeeded -> {
          this.handler = succeeded.extraData();
          this.publishState(new HBStateConnectionSucceeded(succeeded.message()));
          this.publishState(new HBStateConnected());
          LOG.debug("Login succeeded");
          yield new HBConnectionSucceeded<>(succeeded.message(), Void.class);
        }
      };
    } catch (final InterruptedException e) {
      this.publishState(new HBStateConnectionFailed(
        Optional.of(e),
        Optional.empty()
      ));
      LOG.debug("Login failed {}", e);
      throw e;
    }
  }

  @Override
  public final Flow.Publisher<HBStateType> state()
  {
    return this.statePublisher;
  }

  @Override
  public final void disconnect()
    throws X
  {
    this.checkNotClosingOrClosed();

    final var h = this.handler;
    if (!h.isClosed()) {
      try {
        h.close();
      } finally {
        this.handler = this.disconnectedHandler;
        this.publishState(new HBStateDisconnected());
      }
    }
  }

  @Override
  public final boolean isClosed()
  {
    return this.stateNow() instanceof HBStateClosed;
  }

  @Override
  public final void close()
    throws X
  {
    LOG.trace("Close requested");

    synchronized (this.stateLock) {
      final var state = this.stateNow;
      if (state.isClosingOrClosed()) {
        return;
      }
      this.stateNow = new HBStateClosing();
    }

    try {
      LOG.trace("Close starting");
      this.statePublisher.submit(new HBStateClosed());
      this.statePublisher.close();
    } finally {
      synchronized (this.stateLock) {
        this.stateNow = new HBStateClosed();
      }
      logStateChange(new HBStateClosing(), new HBStateClosed());
      LOG.trace("Close completed");
    }
  }

  /**
   * Check that this client is not closing or has not closed.
   */

  protected final void checkNotClosingOrClosed()
  {
    synchronized (this.stateLock) {
      final var state = this.stateNow();
      if (state.isClosingOrClosed()) {
        throw new IllegalStateException("Client is closed!");
      }
    }
  }

  @Override
  public final HBReadType<M> receive(
    final Duration timeout)
    throws X, InterruptedException
  {
    return this.handler.receive(timeout);
  }

  @Override
  public final void send(
    final M message)
    throws X, InterruptedException
  {
    this.handler.send(message);
  }

  @Override
  public final void sendAndForget(
    final M message)
    throws X, InterruptedException
  {
    this.handler.sendAndForget(message);
  }

  @Override
  public final M sendAndWait(
    final M message,
    final Duration timeout)
    throws X, InterruptedException, TimeoutException
  {
    return this.handler.sendAndWait(message, timeout);
  }

  private void publishState(
    final HBStateType newState)
  {
    final HBStateType stateThen;
    synchronized (this.stateLock) {
      stateThen = this.stateNow();
      if (stateThen instanceof HBStateClosed) {
        return;
      }
    }

    logStateChange(stateThen, newState);

    synchronized (this.stateLock) {
      this.stateNow = newState;
    }
    this.statePublisher.submit(newState);
  }

  private static void logStateChange(
    final HBStateType oldState,
    final HBStateType newState)
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace("State {} -> {}", oldState, newState);
    }
  }
}
