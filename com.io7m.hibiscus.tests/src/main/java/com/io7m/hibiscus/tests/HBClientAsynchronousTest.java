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


package com.io7m.hibiscus.tests;

import com.io7m.hibiscus.api.HBClientSynchronousType;
import com.io7m.hibiscus.api.HBCommandType;
import com.io7m.hibiscus.api.HBCredentialsType;
import com.io7m.hibiscus.api.HBEventType;
import com.io7m.hibiscus.api.HBResponseType;
import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBStateType;
import com.io7m.hibiscus.api.HBStateType.HBStateClosed;
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
import com.io7m.hibiscus.basic.HBClientAsynchronous;
import com.io7m.hibiscus.basic.HBClientHandlerType;
import com.io7m.hibiscus.basic.HBClientNewHandler;
import com.io7m.hibiscus.basic.HBClientSynchronous;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Timeout(value = 5L, unit = TimeUnit.SECONDS)
public final class HBClientAsynchronousTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(HBClientAsynchronousTest.class);

  private QWebServerType server;
  private HBResultFailure<HBResponseType, HBResponseType> failureCommand;
  private HBResultSuccess<HBResponseType, HBResponseType> successCommand;
  private HBResultFailure<
    HBClientNewHandler<
      Exception,
      HBCommandType,
      HBResponseType,
      HBResponseType,
      HBResponseType,
      HBEventType,
      HBCredentialsType>, HBResponseType> failureLogin;
  private HBResultSuccess<
    HBClientNewHandler<
      Exception,
      HBCommandType,
      HBResponseType,
      HBResponseType,
      HBResponseType,
      HBEventType,
      HBCredentialsType>, HBResponseType> successLogin;
  private HBClientSynchronousType<
    Exception,
    HBCommandType,
    HBResponseType,
    HBResponseType,
    HBResponseType,
    HBEventType,
    HBCredentialsType> delegate;
  private HBCredentialsType credentials;
  private HandlerType handler0;
  private HandlerType handler1;
  private TestInfo testInfo;

  @BeforeEach
  public void setup(
    final TestInfo inTestInfo)
    throws Exception
  {
    this.testInfo = inTestInfo;
    LOG.debug("{}: test setup", this.testInfo.getDisplayName());

    this.server =
      QWebServers.createServer(20000);

    this.handler0 =
      mock(HandlerType.class);
    this.handler1 =
      mock(HandlerType.class);

    when(this.handler0.onIsConnected())
      .thenReturn(Boolean.FALSE);
    when(this.handler1.onIsConnected())
      .thenReturn(Boolean.TRUE);

    this.credentials =
      mock(HBCredentialsType.class);
    this.delegate =
      new HBClientSynchronous<>(this.handler0, HBClientAsynchronousTest::ofException);

    this.failureCommand =
      new HBResultFailure<>(mock(HBResponseType.class));
    this.successCommand =
      new HBResultSuccess<>(mock(HBResponseType.class));

    this.failureLogin =
      new HBResultFailure<>(mock(HBResponseType.class));
    this.successLogin =
      new HBResultSuccess<>(
        new HBClientNewHandler<>(
          this.handler1,
          mock(HBResponseType.class)
        ));
  }

  private static <RF extends HBResponseType> RF ofException(
    final Throwable throwable)
  {
    return mock();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    LOG.debug("{}: test teardown", this.testInfo.getDisplayName());
    this.server.close();
  }

  /**
   * If a client signals a login failure, login fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFails()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.failureLogin);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertFalse(result.isSuccess());
      assertInstanceOf(HBStateExecutingLoginFailed.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginFailed.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  private void waitForClose(
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient)
    throws InterruptedException
  {
    LOG.debug("{}: waiting for close", this.testInfo.getDisplayName());

    for (int index = 0; index < 1000_000_00; ++index) {
      LOG.debug(
        "{}: still waiting ({})",
        this.testInfo.getDisplayName(),
        leakedClient.stateNow()
      );

      if (leakedClient.isClosed()) {
        break;
      }
      sleep();
    }

    LOG.debug(
      "{}: waited for close successfully",
      this.testInfo.getDisplayName());
  }

  private static void sleep()
    throws InterruptedException
  {
    Thread.sleep(10L);
  }

  /**
   * If a client raises an exception on login, login fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsExceptionally()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenThrow(new NullPointerException("Null!"));

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var future =
        client.loginAsync(this.credentials);
      final var result =
        future.handleAsync((i, x) -> x);

      assertInstanceOf(NullPointerException.class, result.get());
      assertInstanceOf(HBStateExecutingLoginFailed.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginFailed.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * If a client signals a login success, login succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSucceeds()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertTrue(result.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * If a client signals a login failure, login fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsOrElseThrows()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.failureLogin);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var ex =
        assertThrows(
          ExecutionException.class,
          () -> client.loginAsyncOrElseThrow(
              this.credentials,
              HExampleException::new)
            .get()
        );

      assertEquals(
        this.failureLogin.result().toString(),
        ex.getCause().getMessage()
      );
      assertInstanceOf(HBStateExecutingLoginFailed.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginFailed.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * If a client signals a login success, login succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSucceedsOrElseThrows()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      client.loginAsyncOrElseThrow(this.credentials, HExampleException::new)
        .get();

      assertInstanceOf(HBStateConnected.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * If a client signals a command failure, the command fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandFails()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.failureCommand);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      final var result1 =
        client.executeAsync(mock()).get();

      assertFalse(result1.isSuccess());
      assertInstanceOf(HBStateExecutingCommandFailed.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommand.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommandFailed.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * If a client signals a command failure, the command fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandFailsOrElseThrows()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.failureCommand);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      final var ex =
        assertThrows(ExecutionException.class, () -> {
          client.executeAsyncOrElseThrow(mock(), HExampleException::new)
            .get();
        });

      assertEquals(
        this.failureCommand.result().toString(),
        ex.getCause().getMessage()
      );

      assertInstanceOf(HBStateExecutingCommandFailed.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommand.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommandFailed.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * If a client signals a command success, the command succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandSucceeds()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.successCommand);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      final var result1 =
        client.executeAsync(mock()).get();

      assertTrue(result1.isSuccess());
      assertInstanceOf(
        HBStateExecutingCommandSucceeded.class,
        client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommand.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommandSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());

    assertThrows(IllegalStateException.class, () -> {
      leakedClient.loginAsync(mock());
    });
    assertThrows(IllegalStateException.class, () -> {
      leakedClient.executeAsync(mock());
    });
  }

  /**
   * If a client signals a command success, the command succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandSucceedsOrElseThrows()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.successCommand);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      final var result1 =
        client.executeAsyncOrElseThrow(mock(), HExampleException::new)
          .get();

      assertInstanceOf(
        HBStateExecutingCommandSucceeded.class,
        client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommand.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommandSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());

    assertThrows(IllegalStateException.class, () -> {
      leakedClient.loginAsync(mock());
    });
    assertThrows(IllegalStateException.class, () -> {
      leakedClient.executeAsync(mock());
    });
  }

  /**
   * If a client signals a command failure, the command fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandExceptional()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenThrow(new NullPointerException("Null!"));

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      final var future =
        client.executeAsync(mock());
      final var result1 =
        future.handleAsync((i, x) -> x);

      assertInstanceOf(NullPointerException.class, result1.get());
      assertInstanceOf(HBStateExecutingCommandFailed.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommand.class, states.remove(0));
    assertInstanceOf(HBStateExecutingCommandFailed.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, states.remove(0));

    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * Doing nothing does nothing.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandNothing()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * Disconnection works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testDisconnectSucceeds()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertTrue(result.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      client.disconnectAsync()
        .get();

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * Disconnection can fail.
   *
   * @throws Exception On errors
   */

  @Test
  public void testDisconnectExceptional()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      doThrow(new NullPointerException("Null!"))
        .when(this.handler1).onDisconnect();

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertTrue(result.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      final var future =
        client.disconnectAsync();
      final var result1 =
        future.handleAsync((i, x) -> x);

      assertInstanceOf(NullPointerException.class, result1.get());
      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStateDisconnected.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());
  }

  /**
   * If a client signals that polling events succeeded, the events are
   * delivered.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEventPollingSucceeds()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;
    final List<HBEventType> events;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      events = Collections.synchronizedList(new ArrayList<>());
      client.state().subscribe(new HPerpetualSubscriber<>(states::add));
      client.events().subscribe(new HPerpetualSubscriber<>(events::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onPollEvents())
        .thenReturn(List.of(
          new HExampleEvent(0),
          new HExampleEvent(1),
          new HExampleEvent(2)
        ));

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      client.pollEvents().get();
      assertInstanceOf(HBStatePollingEventsSucceeded.class, client.stateNow());

      assertEquals(
        List.of(
          new HExampleEvent(0),
          new HExampleEvent(1),
          new HExampleEvent(2)
        ),
        events
      );
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStatePollingEvents.class, states.remove(0));
    assertInstanceOf(HBStatePollingEventsSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());

    assertThrows(IllegalStateException.class, () -> {
      leakedClient.loginAsync(mock());
    });
    assertThrows(IllegalStateException.class, () -> {
      leakedClient.executeAsync(mock());
    });
  }

  /**
   * If a client signals that polling events failed, the events are not
   * delivered.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEventPollingFails()
    throws Exception
  {
    final HBClientAsynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBStateType<?, ?, ?, ?>> states;
    final List<HBEventType> events;

    try (var client = new HBClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      events = Collections.synchronizedList(new ArrayList<>());
      client.state().subscribe(new HPerpetualSubscriber<>(states::add));
      client.events().subscribe(new HPerpetualSubscriber<>(events::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      doThrow(new NullPointerException("Null!"))
        .when(this.handler1)
        .onPollEvents();

      assertInstanceOf(HBStateDisconnected.class, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertInstanceOf(HBStateConnected.class, client.stateNow());

      assertThrows(ExecutionException.class, () -> client.pollEvents().get());
      assertInstanceOf(HBStatePollingEventsFailed.class, client.stateNow());
      assertEquals(List.of(), events);
    }

    this.waitForClose(leakedClient);

    assertInstanceOf(HBStateExecutingLogin.class, states.remove(0));
    assertInstanceOf(HBStateExecutingLoginSucceeded.class, states.remove(0));
    assertInstanceOf(HBStateConnected.class, states.remove(0));
    assertInstanceOf(HBStatePollingEvents.class, states.remove(0));
    assertInstanceOf(HBStatePollingEventsFailed.class, states.remove(0));
    assertInstanceOf(HBStateClosed.class, leakedClient.stateNow());

    assertThrows(IllegalStateException.class, () -> {
      leakedClient.loginAsync(mock());
    });
    assertThrows(IllegalStateException.class, () -> {
      leakedClient.executeAsync(mock());
    });
  }

  interface HandlerType extends
    HBClientHandlerType<
      Exception,
      HBCommandType,
      HBResponseType,
      HBResponseType,
      HBResponseType,
      HBEventType,
      HBCredentialsType>
  {

  }
}
