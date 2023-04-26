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

import com.io7m.hibiscus.api.HBCommandType;
import com.io7m.hibiscus.api.HBCredentialsType;
import com.io7m.hibiscus.api.HBEventType;
import com.io7m.hibiscus.api.HBResponseType;
import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBState;
import com.io7m.hibiscus.basic.HBClientHandlerType;
import com.io7m.hibiscus.basic.HBClientNewHandler;
import com.io7m.hibiscus.basic.HBClientSynchronous;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class HBClientSynchronousTest
{
  private QWebServerType server;
  private HBResultFailure<HBResponseType, HBResponseType> failureCommand;
  private HBResultSuccess<HBResponseType, HBResponseType> successCommand;
  private HandlerType handler0;
  private HandlerType handler1;
  private HBCredentialsType credentials;
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

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.server =
      QWebServers.createServer(20000);

    this.credentials =
      mock(HBCredentialsType.class);
    this.handler0 =
      mock(HandlerType.class);
    this.handler1 =
      mock(HandlerType.class);

    when(this.handler0.onIsConnected())
      .thenReturn(Boolean.FALSE);
    when(this.handler1.onIsConnected())
      .thenReturn(Boolean.TRUE);

    this.failureLogin =
      new HBResultFailure<>(mock(HBResponseType.class));
    this.successLogin =
      new HBResultSuccess<>(
        new HBClientNewHandler<>(
          this.handler1,
          mock(HBResponseType.class)
        ));

    this.failureCommand =
      new HBResultFailure<>(mock(HBResponseType.class));
    this.successCommand =
      new HBResultSuccess<>(mock(HBResponseType.class));
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.server.close();
  }

  /**
   * If a handler signals a login failure, login fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFails()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.failureLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.login(this.credentials);

      assertFalse(result.isSuccess());
      assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, client.stateNow());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * If a handler signals a login failure, login fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsOrElseThrows()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.failureLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var ex =
        assertThrows(HExampleException.class, () -> {
          client.loginOrElseThrow(this.credentials, HExampleException::new);
        });

      assertEquals(
        this.failureLogin.result().toString(),
        ex.getMessage()
      );
      assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, client.stateNow());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * If a handler signals a login success, login succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSucceeds()
    throws Exception
  {
    final List<HBState> states;
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      final var result =
        client.login(this.credentials);
      assertTrue(result.isSuccess());
      assertEquals(CLIENT_CONNECTED, client.stateNow());
      assertTrue(client.isConnected());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * Login interruption is signalled.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginInterrupted()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenThrow(new InterruptedException("Interrupted!"));

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      assertThrows(InterruptedException.class, () -> {
        client.login(this.credentials);
      });

      assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, client.stateNow());
      assertFalse(client.isConnected());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_FAILED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * If a handler signals a command failure, the command fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandFails()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      /*
       * On login success, the handler is replaced with handler 1.
       */

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(Mockito.any()))
        .thenReturn(this.failureCommand);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      client.login(this.credentials);
      assertTrue(client.isConnected());

      final var result =
        client.execute(mock(HBCommandType.class));

      assertFalse(result.isSuccess());
      assertTrue(client.isConnected());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * If a handler signals a command failure, the command fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandFailsOrElseThrows()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      /*
       * On login success, the handler is replaced with handler 1.
       */

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(Mockito.any()))
        .thenReturn(this.failureCommand);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      client.login(this.credentials);
      assertTrue(client.isConnected());

      final var ex =
        assertThrows(HExampleException.class, () -> {
          client.executeOrElseThrow(
            mock(HBCommandType.class),
            HExampleException::new
          );
        });

      assertEquals(
        this.failureCommand.result().toString(),
        ex.getMessage()
      );
      assertTrue(client.isConnected());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * If a handler signals a command success, the command succeeds.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandSucceeds()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      /*
       * On login success, the handler is replaced with handler1.
       */

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(Mockito.any()))
        .thenReturn(this.successCommand);

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      client.login(this.credentials);
      assertTrue(client.isConnected());

      final var result =
        client.execute(mock(HBCommandType.class));

      assertTrue(result.isSuccess());
      assertTrue(client.isConnected());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_SUCCEEDED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());

    assertThrows(IllegalStateException.class, () -> {
      leakedClient.login(mock());
    });
    assertThrows(IllegalStateException.class, () -> {
      leakedClient.execute(mock());
    });
  }

  /**
   * Command interruption is signalled.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandInterrupted()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      /*
       * On login success, the handler is replaced with handler1.
       */

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onExecuteCommand(Mockito.any()))
        .thenThrow(new InterruptedException("Interrupted!"));

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());

      client.login(this.credentials);
      assertTrue(client.isConnected());

      assertThrows(InterruptedException.class, () -> {
        client.execute(mock(HBCommandType.class));
      });

      assertTrue(client.isConnected());
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND, states.remove(0));
    assertEquals(CLIENT_EXECUTING_COMMAND_FAILED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * Disconnection interruption is signalled.
   *
   * @throws Exception On errors
   */

  @Test
  public void testDisconnectInterrupted()
    throws Exception
  {
    final List<HBState> states;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      states = Collections.synchronizedList(new ArrayList<>());
      client.state()
        .subscribe(new HPerpetualSubscriber<>(states::add));

      /*
       * On login success, the handler is replaced with handler1.
       */

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);

      doThrow(new InterruptedException("Interrupted!"))
        .when(this.handler1).onDisconnect();

      client.login(this.credentials);
      assertTrue(client.isConnected());

      assertThrows(InterruptedException.class, client::disconnect);
    }
  }

  /**
   * If a handler signals polling events success, the events are delivered.
   *
   * @throws Exception On errors
   */

  @Test
  public void testPollEventsSucceeds()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;
    final List<HBEventType> events;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      events = Collections.synchronizedList(new ArrayList<>());

      client.state().subscribe(new HPerpetualSubscriber<>(states::add));
      client.events().subscribe(new HPerpetualSubscriber<>(events::add));

      /*
       * On login success, the handler is replaced with handler1.
       */

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);
      when(this.handler1.onPollEvents())
        .thenReturn(List.of(
          new HExampleEvent(0),
          new HExampleEvent(1),
          new HExampleEvent(2)
        ));

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      client.login(this.credentials);
      assertTrue(client.isConnected());

      client.pollEvents();
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

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS_SUCCEEDED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
  }

  /**
   * If a handler signals polling events fails, the events are not delivered.
   *
   * @throws Exception On errors
   */

  @Test
  public void testPollEventsFails()
    throws Exception
  {
    final HBClientSynchronous<?, ?, ?, ?, ?, ?, ?> leakedClient;
    final List<HBState> states;
    final List<HBEventType> events;

    try (var client = new HBClientSynchronous<>(this.handler0)) {
      leakedClient = client;
      states = Collections.synchronizedList(new ArrayList<>());
      events = Collections.synchronizedList(new ArrayList<>());

      client.state().subscribe(new HPerpetualSubscriber<>(states::add));
      client.events().subscribe(new HPerpetualSubscriber<>(events::add));

      /*
       * On login success, the handler is replaced with handler1.
       */

      when(this.handler0.onExecuteLogin(Mockito.any()))
        .thenReturn(this.successLogin);

      doThrow(new NullPointerException("Null!"))
        .when(this.handler1)
        .onPollEvents();

      assertEquals(CLIENT_DISCONNECTED, client.stateNow());
      assertFalse(client.isConnected());

      client.login(this.credentials);
      assertTrue(client.isConnected());

      assertThrows(NullPointerException.class, client::pollEvents);
      assertEquals(CLIENT_POLLING_EVENTS_FAILED, client.stateNow());
      assertEquals(List.of(), events);
    }

    assertEquals(CLIENT_EXECUTING_LOGIN, states.remove(0));
    assertEquals(CLIENT_EXECUTING_LOGIN_SUCCEEDED, states.remove(0));
    assertEquals(CLIENT_CONNECTED, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS, states.remove(0));
    assertEquals(CLIENT_POLLING_EVENTS_FAILED, states.remove(0));

    assertEquals(CLIENT_CLOSED, leakedClient.stateNow());
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
