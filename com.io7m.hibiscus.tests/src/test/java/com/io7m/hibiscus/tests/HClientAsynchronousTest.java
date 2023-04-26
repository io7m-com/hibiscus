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
import com.io7m.hibiscus.api.HBState;
import com.io7m.hibiscus.basic.HClientAsynchronous;
import com.io7m.hibiscus.basic.HClientHandlerType;
import com.io7m.hibiscus.basic.HClientNewHandler;
import com.io7m.hibiscus.basic.HClientSynchronous;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.io7m.hibiscus.api.HBState.CLIENT_CLOSED;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class HClientAsynchronousTest
{
  private QWebServerType server;
  private HBResultFailure<HBResponseType, HBResponseType> failureCommand;
  private HBResultSuccess<HBResponseType, HBResponseType> successCommand;
  private HBResultFailure<
    HClientNewHandler<
      Exception,
      HBCommandType,
      HBResponseType,
      HBResponseType,
      HBEventType,
      HBCredentialsType>, HBResponseType> failureLogin;
  private HBResultSuccess<
    HClientNewHandler<
      Exception,
      HBCommandType,
      HBResponseType,
      HBResponseType,
      HBEventType,
      HBCredentialsType>, HBResponseType> successLogin;
  private HBClientSynchronousType<
    Exception,
    HBCommandType,
    HBResponseType,
    HBResponseType,
    HBEventType,
    HBCredentialsType> delegate;
  private HBCredentialsType credentials;
  private HandlerType handler0;
  private HandlerType handler1;

  @BeforeEach
  public void setup()
    throws Exception
  {
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
      new HClientSynchronous<>(this.handler0);

    this.failureCommand =
      new HBResultFailure<>(mock(HBResponseType.class));
    this.successCommand =
      new HBResultSuccess<>(mock(HBResponseType.class));

    this.failureLogin =
      new HBResultFailure<>(mock(HBResponseType.class));
    this.successLogin =
      new HBResultSuccess<>(
        new HClientNewHandler<>(
          this.handler1,
          mock(HBResponseType.class)
        ));
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.failureLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertFalse(result.isSuccess());
      assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenThrow(new NullPointerException("Null!"));

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var future =
        client.loginAsync(this.credentials);
      final var result =
        future.handleAsync((i, x) -> x);

      assertInstanceOf(NullPointerException.class, result.get());
      assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertTrue(result.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.failureLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
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
      assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      client.loginAsyncOrElseThrow(this.credentials, HExampleException::new)
        .get();

      assertEquals(CLIENT_CONNECTED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.failureCommand);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      final var result1 =
        client.executeAsync(mock()).get();

      assertFalse(result1.isSuccess());
      assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.failureCommand);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      final var ex =
        assertThrows(ExecutionException.class, () -> {
          client.executeAsyncOrElseThrow(mock(), HExampleException::new)
            .get();
        });

      assertEquals(
        this.failureCommand.result().toString(),
        ex.getCause().getMessage()
      );

      assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.successCommand);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      final var result1 =
        client.executeAsync(mock()).get();

      assertTrue(result1.isSuccess());
      assertEquals(CLIENT_EXECUTING_COMMAND_SUCCEEDED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());

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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenReturn(this.successCommand);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      final var result1 =
        client.executeAsyncOrElseThrow(mock(), HExampleException::new)
          .get();

      assertEquals(CLIENT_EXECUTING_COMMAND_SUCCEEDED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());

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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(any()))
        .thenThrow(new NullPointerException("Null!"));

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      final var future =
        client.executeAsync(mock());
      final var result1 =
        future.handleAsync((i, x) -> x);

      assertInstanceOf(NullPointerException.class, result1.get());
      assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      Thread.sleep(1_000L);
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertTrue(result.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      client.disconnectAsync()
        .get();

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);

      doThrow(new NullPointerException("Null!"))
        .when(this.handler1).onDisconnect();

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.loginAsync(this.credentials)
          .get();

      assertTrue(result.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      final var future =
        client.disconnectAsync();
      final var result1 =
        future.handleAsync((i, x) -> x);

      assertInstanceOf(NullPointerException.class, result1.get());
      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      Thread.sleep(20L);
    }

    Thread.sleep(20L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_DISCONNECTED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * If a client signals that polling events succeeded, the events are delivered.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEventPollingSucceeds()
    throws Exception
  {
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;
    final ArrayList<HBEventType> events;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      events = new ArrayList<>();
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

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      client.pollEvents().get();
      assertEquals(CLIENT_POLLING_EVENTS_SUCCEEDED, client.stateNow());

      assertEquals(
        List.of(
          new HExampleEvent(0),
          new HExampleEvent(1),
          new HExampleEvent(2)
        ),
        events
      );
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());

    assertThrows(IllegalStateException.class, () -> {
      leakedClient.loginAsync(mock());
    });
    assertThrows(IllegalStateException.class, () -> {
      leakedClient.executeAsync(mock());
    });
  }

  /**
   * If a client signals that polling events failed, the events are not delivered.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEventPollingFails()
    throws Exception
  {
    final HClientAsynchronous<?, ?, ?, ?, ?, ?> leakedClient;
    final ArrayList<HBState> states;
    final ArrayList<HBEventType> events;

    try (var client = new HClientAsynchronous<>(this.delegate, "t")) {
      leakedClient = client;
      states = new ArrayList<>();
      events = new ArrayList<>();
      client.state().subscribe(new HPerpetualSubscriber<>(states::add));
      client.events().subscribe(new HPerpetualSubscriber<>(events::add));

      when(this.handler0.onExecuteLogin(any()))
        .thenReturn(this.successLogin);
      doThrow(new NullPointerException("Null!"))
        .when(this.handler1)
        .onPollEvents();

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result0 =
        client.loginAsync(this.credentials).get();

      assertTrue(result0.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());

      assertThrows(ExecutionException.class, () -> client.pollEvents().get());
      assertEquals(CLIENT_POLLING_EVENTS_FAILED, client.stateNow());
      assertEquals(List.of(), events);
    }

    Thread.sleep(10L);
    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS_FAILED, states.remove(0));
    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());

    assertThrows(IllegalStateException.class, () -> {
      leakedClient.loginAsync(mock());
    });
    assertThrows(IllegalStateException.class, () -> {
      leakedClient.executeAsync(mock());
    });
  }

  interface HandlerType extends
    HClientHandlerType<
      Exception,
      HBCommandType,
      HBResponseType,
      HBResponseType,
      HBEventType,
      HBCredentialsType>
  {

  }
}
