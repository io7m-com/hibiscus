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


package com.io7m.hibiscus.tests.it;

import com.io7m.hibiscus.api.HBStateType.HBStateConnected;
import com.io7m.hibiscus.api.HBStateType.HBStateConnectionFailed;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import com.io7m.hibiscus.examples.http0.EHTTP0ClientType;
import com.io7m.hibiscus.examples.http0.EHTTP0Clients;
import com.io7m.hibiscus.examples.http0.EHTTP0CommandHello;
import com.io7m.hibiscus.examples.http0.EHTTP0Configuration;
import com.io7m.hibiscus.examples.http0.EHTTP0ConnectionParameters;
import com.io7m.hibiscus.examples.http0.EHTTP0Exception;
import com.io7m.hibiscus.examples.http0.EHTTP0ResponseFailure;
import com.io7m.hibiscus.examples.http0.EHTTP0ResponseOK;
import com.io7m.hibiscus.examples.http0.EHTTP0Server;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@Timeout(value = 5_000L, unit = TimeUnit.SECONDS)
public final class EHTTP0IT
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EHTTP0IT.class);

  private static final Duration STARTUP_TIMEOUT =
    Duration.ofSeconds(5L);

  private static final int PORT = 47000;
  private static EHTTP0Server SERVER;
  private static InetSocketAddress ADDRESS;

  private EHTTP0Clients clients;
  private EHTTP0ClientType client;
  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private LinkedBlockingDeque<String> clientStates;

  @BeforeAll
  public static void setupOnce()
    throws Exception
  {
    ADDRESS =
      new InetSocketAddress("localhost", PORT);
    SERVER =
      new EHTTP0Server(ADDRESS);

    assertTimeoutPreemptively(STARTUP_TIMEOUT, () -> {
      SERVER.start();
    });
  }

  @AfterAll
  public static void teardownOnce()
    throws IOException
  {
    SERVER.close();
  }

  @BeforeEach
  public void setup(
    final TestInfo info)
  {
    LOG.debug("Test: {}", info.getDisplayName());

    this.resources =
      CloseableCollection.create();

    this.clients =
      new EHTTP0Clients();
    this.client =
      this.resources.add(this.clients.create(new EHTTP0Configuration()));

    this.clientStates =
      new LinkedBlockingDeque<>();

    this.client.state()
      .subscribe(new HBPerpetualSubscriber<>(
        state -> this.clientStates.add(state.toString())
      ));
  }

  @AfterEach
  public void tearDown(
    final TestInfo info)
    throws Exception
  {
    LOG.debug("tearDown: Test: {}", info.getDisplayName());

    this.resources.close();
  }

  @Test
  public void testConnectAsk()
    throws Exception
  {
    final var parameters =
      new EHTTP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofSeconds(1L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());

    {
      final var r =
        this.client.ask(
          new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"),
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(EHTTP0ResponseOK.class, r);
    }

    {
      final var r =
        this.client.ask(
          new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"),
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(EHTTP0ResponseOK.class, r);
    }

    {
      final var r =
        this.client.ask(
          new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"),
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(EHTTP0ResponseOK.class, r);
    }

    assertEquals(
      List.of(
        "CONNECTING",
        "CONNECTION_SUCCEEDED",
        "CONNECTED"
      ),
      List.copyOf(this.clientStates)
    );
  }

  @Test
  public void testConnectAskGarbage0()
    throws Exception
  {
    final var parameters =
      new EHTTP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofSeconds(1L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());

    {
      final var r =
        this.client.ask(
          new EHTTP0ResponseOK(UUID.randomUUID(), UUID.randomUUID()),
          Duration.ofSeconds(1L)
        );

      assertInstanceOf(EHTTP0ResponseFailure.class, r);
    }

    assertEquals(
      List.of(
        "CONNECTING",
        "CONNECTION_SUCCEEDED",
        "CONNECTED"
      ),
      List.copyOf(this.clientStates)
    );
  }

  @Test
  public void testConnectSend()
    throws Exception
  {
    final var parameters =
      new EHTTP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofSeconds(1L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());

    this.client.send(new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"));
    this.client.send(new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"));
    this.client.send(new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"));

    assertEquals(
      List.of(
        "CONNECTING",
        "CONNECTION_SUCCEEDED",
        "CONNECTED"
      ),
      List.copyOf(this.clientStates)
    );
  }

  @Test
  public void testConnectFailure0()
    throws Exception
  {
    final var parameters =
      new EHTTP0ConnectionParameters(
        ADDRESS,
        "someone",
        "wrong!",
        Duration.ofSeconds(1L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnectionFailed.class, this.client.stateNow());

    assertEquals(
      List.of(
        "CONNECTING",
        "CONNECTION_FAILED"
      ),
      List.copyOf(this.clientStates)
    );
  }

  @Test
  public void testConnectFailure1()
    throws Exception
  {
    final var parameters =
      new EHTTP0ConnectionParameters(
        new InetSocketAddress(
          ADDRESS.getAddress(),
          9999
        ),
        "someone",
        "wrong!",
        Duration.ofSeconds(1L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnectionFailed.class, this.client.stateNow());

    assertEquals(
      List.of(
        "CONNECTING",
        "CONNECTION_FAILED"
      ),
      List.copyOf(this.clientStates)
    );
  }

  @Test
  public void testNotConnected()
    throws Exception
  {
    final var parameters =
      new EHTTP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofSeconds(1L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());

    assertThrows(EHTTP0Exception.class, () -> {
      this.client.receive(Duration.ZERO);
    });
    assertThrows(EHTTP0Exception.class, () -> {
      this.client.send(new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"));
    });
    assertThrows(EHTTP0Exception.class, () -> {
      this.client.ask(
        new EHTTP0CommandHello(UUID.randomUUID(), "Hello!"),
        Duration.ofSeconds(1L)
      );
    });
  }

  @Test
  public void testConnectConnect()
    throws Exception
  {
    final var parameters =
      new EHTTP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofSeconds(1L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());
  }
}
