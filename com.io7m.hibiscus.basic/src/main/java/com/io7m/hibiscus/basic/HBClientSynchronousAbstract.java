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

import com.io7m.hibiscus.api.HBClientSynchronousType;
import com.io7m.hibiscus.api.HBCommandType;
import com.io7m.hibiscus.api.HBCredentialsType;
import com.io7m.hibiscus.api.HBEventType;
import com.io7m.hibiscus.api.HBResponseType;
import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.api.HBStateType;
import com.io7m.hibiscus.api.HBStateType.HBStateClosed;
import com.io7m.hibiscus.api.HBStateType.HBStateClosing;
import com.io7m.hibiscus.api.HBStateType.HBStateConnected;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import com.io7m.hibiscus.api.HBStateType.HBStateExecutingCommand;
import com.io7m.hibiscus.api.HBStateType.HBStateExecutingCommandFailed;
import com.io7m.hibiscus.api.HBStateType.HBStateExecutingCommandSucceeded;
import com.io7m.hibiscus.api.HBStateType.HBStateExecutingLogin;
import com.io7m.hibiscus.api.HBStateType.HBStateExecutingLoginFailed;
import com.io7m.hibiscus.api.HBStateType.HBStateExecutingLoginSucceeded;
import com.io7m.hibiscus.api.HBStateType.HBStatePollingEvents;
import com.io7m.hibiscus.api.HBStateType.HBStatePollingEventsFailed;
import com.io7m.hibiscus.api.HBStateType.HBStatePollingEventsSucceeded;
import com.io7m.junreachable.UnreachableCodeException;
import net.jcip.annotations.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * The basic synchronous client.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <R>  The type of responses from the server
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

public abstract class HBClientSynchronousAbstract<
  X extends Exception,
  C extends HBCommandType,
  R extends HBResponseType,
  RS extends R,
  RF extends R,
  E extends HBEventType,
  CR extends HBCredentialsType>
  implements HBClientSynchronousType<X, C, R, RS, RF, E, CR>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(HBClientSynchronousAbstract.class);

  private final SubmissionPublisher<E> eventPublisher;
  private final SubmissionPublisher<HBStateType<C, R, RF, CR>> statePublisher;
  private final HBClientHandlerType<X, C, R, RS, RF, E, CR> disconnectedHandler;
  private final HBDirectExecutor executor;
  private final Object stateLock;
  private final HBClientExceptionTransformerType<RF> exceptions;
  @GuardedBy("stateLock")
  private HBStateType<C, R, RF, CR> stateNow;
  private HBClientHandlerType<X, C, R, RS, RF, E, CR> handler;

  /**
   * Construct a client.
   *
   * @param inDisconnectedHandler The handler that will be (re)used in the
   *                              disconnected state.
   * @param inExceptions          A function that can transform exceptions to
   *                              responses
   */

  protected HBClientSynchronousAbstract(
    final HBClientHandlerType<X, C, R, RS, RF, E, CR> inDisconnectedHandler,
    final HBClientExceptionTransformerType<RF> inExceptions)
  {
    this.disconnectedHandler =
      Objects.requireNonNull(inDisconnectedHandler, "disconnectedHandler");
    this.exceptions =
      Objects.requireNonNull(inExceptions, "inExceptions");

    this.executor =
      new HBDirectExecutor();
    this.eventPublisher =
      new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize());
    this.statePublisher =
      new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize());

    this.stateLock =
      new Object();
    this.stateNow =
      new HBStateDisconnected<>();
    this.handler =
      this.disconnectedHandler;
  }

  private static void logStateChange(
    final HBStateType<?, ?, ?, ?> oldState,
    final HBStateType<?, ?, ?, ?> newState)
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace("state {} -> {}", oldState, newState);
    }
  }

  @Override
  public final boolean isClosed()
  {
    return this.stateNow() instanceof HBStateClosed;
  }

  @Override
  public final boolean isConnected()
  {
    return this.handler.onIsConnected();
  }

  @Override
  public final Flow.Publisher<E> events()
  {
    return this.eventPublisher;
  }

  @Override
  public final Flow.Publisher<HBStateType<C, R, RF, CR>> state()
  {
    return this.statePublisher;
  }

  @Override
  public final HBStateType<C, R, RF, CR> stateNow()
  {
    synchronized (this.stateLock) {
      return this.stateNow;
    }
  }

  @Override
  public final void pollEvents()
    throws InterruptedException
  {
    this.checkNotClosingOrClosed();

    this.publishState(new HBStatePollingEvents<>());

    try {
      this.handler.onPollEvents().forEach(this.eventPublisher::submit);
      LOG.debug("polling events succeeded");
      this.publishState(new HBStatePollingEventsSucceeded<>());
    } catch (final Exception e) {
      this.publishState(new HBStatePollingEventsFailed<>());
      LOG.debug("polling events failed");
      throw e;
    }
  }

  @Override
  public final HBResultType<RS, RF> login(
    final CR credentials)
    throws InterruptedException
  {
    Objects.requireNonNull(credentials, "credentials");
    this.checkNotClosingOrClosed();

    this.disconnect();
    this.publishState(new HBStateExecutingLogin<>(credentials));

    final HBResultType<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> result;
    try {
      result = this.handler.onExecuteLogin(credentials);
    } catch (final Throwable e) {
      this.publishState(
        new HBStateExecutingLoginFailed<>(this.exceptions.ofException(e))
      );
      LOG.debug("login failed");
      throw e;
    }

    Objects.requireNonNull(result, "result");

    if (result instanceof HBResultSuccess<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> success) {
      final var r = success.result();
      this.handler = r.newHandler();
      this.publishState(new HBStateExecutingLoginSucceeded<>(r.loginResponse()));
      this.publishState(new HBStateConnected<>());
      LOG.debug("login succeeded");
      return success.map(HBClientNewHandler::loginResponse);
    }

    if (result instanceof HBResultFailure<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> failure) {
      this.publishState(new HBStateExecutingLoginFailed<>(failure.result()));
      LOG.debug("login failed");
      return failure.cast();
    }

    throw new UnreachableCodeException();
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

  private void publishState(
    final HBStateType<C, R, RF, CR> newState)
  {
    final HBStateType<C, R, RF, CR> stateThen;
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

  @Override
  public final HBResultType<RS, RF> execute(
    final C command)
    throws InterruptedException
  {
    Objects.requireNonNull(command, "command");
    this.checkNotClosingOrClosed();

    this.publishState(new HBStateExecutingCommand<>(command));

    final HBResultType<RS, RF> result;
    try {
      result = this.handler.onExecuteCommand(command);
    } catch (final Throwable e) {
      this.publishState(
        new HBStateExecutingCommandFailed<>(
          command,
          this.exceptions.ofException(e)
        )
      );
      LOG.debug("command failed");
      throw e;
    }

    Objects.requireNonNull(result, "result");

    if (result instanceof HBResultSuccess<RS, RF> success) {
      this.publishState(
        new HBStateExecutingCommandSucceeded<>(command, success.result())
      );
      LOG.debug("command succeeded");
    }

    if (result instanceof HBResultFailure<RS, RF> failure) {
      this.publishState(
        new HBStateExecutingCommandFailed<>(command, failure.result())
      );
      LOG.debug("command failed");
    }

    return result;
  }

  @Override
  public final void disconnect()
    throws InterruptedException
  {
    this.checkNotClosingOrClosed();

    if (this.handler.onIsConnected()) {
      try {
        this.handler.onDisconnect();
      } finally {
        this.handler = this.disconnectedHandler;
        this.publishState(new HBStateDisconnected<>());
      }
    }
  }

  /**
   * @return The current handler
   */

  protected final HBClientHandlerType<X, C, R, RS, RF, E, CR> currentHandler()
  {
    return this.handler;
  }

  /**
   * @return The client event publisher
   */

  protected final SubmissionPublisher<E> eventPublisher()
  {
    return this.eventPublisher;
  }

  /**
   * @return The client state publisher
   */

  protected final SubmissionPublisher<HBStateType<C, R, RF, CR>> statePublisher()
  {
    return this.statePublisher;
  }

  @Override
  public final void close()
  {
    LOG.trace("close requested");

    synchronized (this.stateLock) {
      final var state = this.stateNow;
      if (state.isClosingOrClosed()) {
        return;
      }
      this.stateNow = new HBStateClosing<>();
    }

    try {
      LOG.trace("close starting");
      this.statePublisher.submit(new HBStateClosed<>());

      try {
        this.statePublisher.close();
      } finally {
        this.eventPublisher.close();
      }
    } finally {
      synchronized (this.stateLock) {
        this.stateNow = new HBStateClosed<>();
      }
      logStateChange(new HBStateClosing<>(), new HBStateClosed<>());
      LOG.trace("close completed");
    }
  }
}
