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
import com.io7m.hibiscus.api.HBState;
import com.io7m.junreachable.UnreachableCodeException;
import net.jcip.annotations.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import static com.io7m.hibiscus.api.HBState.CLIENT_CLOSED;
import static com.io7m.hibiscus.api.HBState.CLIENT_CLOSING;
import static com.io7m.hibiscus.api.HBState.CLIENT_CONNECTED;
import static com.io7m.hibiscus.api.HBState.CLIENT_DISCONNECTED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_COMMAND;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_COMMAND_FAILED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_COMMAND_SUCCEEDED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_LOGIN;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_LOGIN_FAILED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_LOGIN_SUCCEEDED;
import static com.io7m.hibiscus.api.HBState.CLIENT_POLLING_EVENTS;
import static com.io7m.hibiscus.api.HBState.CLIENT_POLLING_EVENTS_FAILED;
import static com.io7m.hibiscus.api.HBState.CLIENT_POLLING_EVENTS_SUCCEEDED;

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
  private final SubmissionPublisher<HBState> statePublisher;
  private final HBClientHandlerType<X, C, R, RS, RF, E, CR> disconnectedHandler;
  private final HBDirectExecutor executor;
  private final Object stateLock;
  @GuardedBy("stateLock")
  private HBState stateNow;
  private HBClientHandlerType<X, C, R, RS, RF, E, CR> handler;

  /**
   * Construct a client.
   *
   * @param inDisconnectedHandler The handler that will be (re)used in the
   *                              disconnected state.
   */

  protected HBClientSynchronousAbstract(
    final HBClientHandlerType<X, C, R, RS, RF, E, CR> inDisconnectedHandler)
  {
    this.disconnectedHandler =
      Objects.requireNonNull(inDisconnectedHandler, "disconnectedHandler");

    this.executor =
      new HBDirectExecutor();
    this.eventPublisher =
      new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize());
    this.statePublisher =
      new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize());

    this.stateLock =
      new Object();
    this.stateNow =
      CLIENT_DISCONNECTED;
    this.handler =
      this.disconnectedHandler;
  }

  private static void logStateChange(
    final HBState oldState,
    final HBState newState)
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace("state {} -> {}", oldState, newState);
    }
  }

  @Override
  public final boolean isClosed()
  {
    return this.stateNow() == CLIENT_CLOSED;
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
  public final Flow.Publisher<HBState> state()
  {
    return this.statePublisher;
  }

  @Override
  public final HBState stateNow()
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

    this.publishState(CLIENT_POLLING_EVENTS);

    try {
      this.handler.onPollEvents().forEach(this.eventPublisher::submit);
      LOG.debug("polling events succeeded");
      this.publishState(CLIENT_POLLING_EVENTS_SUCCEEDED);
    } catch (final Exception e) {
      this.publishState(CLIENT_POLLING_EVENTS_FAILED);
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
    this.publishState(CLIENT_EXECUTING_LOGIN);

    final HBResultType<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> result;
    try {
      result = this.handler.onExecuteLogin(credentials);
    } catch (final Exception e) {
      this.publishState(CLIENT_EXECUTING_LOGIN_FAILED);
      LOG.debug("login failed");
      throw e;
    }

    Objects.requireNonNull(result, "result");

    if (result instanceof final HBResultSuccess<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> success) {
      this.handler = success.result().newHandler();
      this.publishState(CLIENT_EXECUTING_LOGIN_SUCCEEDED);
      this.publishState(CLIENT_CONNECTED);
      LOG.debug("login succeeded");
      this.onLoginExecuteSucceeded(
        credentials,
        success.result().loginResponse()
      );
      return success.map(HBClientNewHandler::loginResponse);
    }
    if (result instanceof final HBResultFailure<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> failure) {
      this.publishState(CLIENT_EXECUTING_LOGIN_FAILED);
      LOG.debug("login failed");
      this.onLoginExecuteFailed(
        credentials,
        failure.result()
      );
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
      if (state == CLIENT_CLOSING || state == CLIENT_CLOSED) {
        throw new IllegalStateException("Client is closed!");
      }
    }
  }

  private void publishState(
    final HBState newState)
  {
    final HBState stateThen;
    synchronized (this.stateLock) {
      stateThen = this.stateNow();
      if (stateThen == CLIENT_CLOSED) {
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

    this.publishState(CLIENT_EXECUTING_COMMAND);

    final HBResultType<RS, RF> result;
    try {
      result = this.handler.onExecuteCommand(command);
    } catch (final Exception e) {
      this.publishState(CLIENT_EXECUTING_COMMAND_FAILED);
      LOG.debug("command failed");
      throw e;
    }

    Objects.requireNonNull(result, "result");

    if (result.isSuccess()) {
      this.publishState(CLIENT_EXECUTING_COMMAND_SUCCEEDED);
      LOG.debug("command succeeded");
      this.onCommandExecuteSucceeded(
        command,
        ((HBResultSuccess<RS, RF>) result).result()
      );
    } else {
      this.publishState(CLIENT_EXECUTING_COMMAND_FAILED);
      LOG.debug("command failed");
      this.onCommandExecuteFailed(
        command,
        ((HBResultFailure<RS, RF>) result).result()
      );
    }

    return result;
  }

  /**
   * A method called when a command is executed successfully.
   *
   * @param command The command
   * @param result  The result
   */

  protected abstract void onCommandExecuteSucceeded(
    C command,
    RS result
  );

  /**
   * A method called when a command fails.
   *
   * @param command The command
   * @param result  The result
   */

  protected abstract void onCommandExecuteFailed(
    C command,
    RF result
  );

  /**
   * A method called when a login attempt completes successfully.
   *
   * @param credentials The credentials
   * @param result      The result
   */

  protected abstract void onLoginExecuteSucceeded(
    CR credentials,
    RS result);

  /**
   * A method called when a login attempt fails.
   *
   * @param credentials The credentials
   * @param result      The result
   */

  protected abstract void onLoginExecuteFailed(
    CR credentials,
    RF result);

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
        this.publishState(CLIENT_DISCONNECTED);
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

  protected final SubmissionPublisher<HBState> statePublisher()
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
      this.stateNow = CLIENT_CLOSING;
    }

    try {
      LOG.trace("close starting");
      this.statePublisher.submit(CLIENT_CLOSED);

      try {
        this.statePublisher.close();
      } finally {
        this.eventPublisher.close();
      }
    } finally {
      synchronized (this.stateLock) {
        this.stateNow = CLIENT_CLOSED;
      }
      logStateChange(CLIENT_CLOSING, CLIENT_CLOSED);
      LOG.trace("close completed");
    }
  }
}
