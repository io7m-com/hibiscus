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


package com.io7m.hibiscus.tests.examples.tcp0;

import com.io7m.hibiscus.api.HBStateType.HBStateConnected;
import com.io7m.hibiscus.api.HBStateType.HBStateConnectionFailed;
import com.io7m.hibiscus.api.HBStateType.HBStateDisconnected;
import com.io7m.hibiscus.tests.HPerpetualSubscriber;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Timeout(value = 5_000L, unit = TimeUnit.SECONDS)
public final class ExTCP0Test
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ExTCP0Test.class);

  private static final int PORT = 46000;
  private static ETCP0Server SERVER;
  private static InetSocketAddress ADDRESS;

  private ETCP0Clients clients;
  private ETCP0ClientType client;
  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private LinkedBlockingDeque<String> clientStates;

  @BeforeAll
  public static void setupOnce()
    throws IOException, InterruptedException
  {
    ADDRESS =
      new InetSocketAddress("localhost", PORT);
    SERVER =
      new ETCP0Server(ADDRESS);

    final var latch = new CountDownLatch(1);
    Thread.startVirtualThread(() -> {
      try {
        SERVER.start(latch);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    });
    latch.await(60L, TimeUnit.SECONDS);
    LOG.debug("Server up!");
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
      new ETCP0Clients();
    this.client =
      this.resources.add(this.clients.create(new ETCP0Configuration()));

    this.clientStates =
      new LinkedBlockingDeque<>();

    this.client.state()
      .subscribe(new HPerpetualSubscriber<>(
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
      new ETCP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofMillis(10L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());

    {
      final var r =
        this.client.ask(
          new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));

      assertInstanceOf(ETCP0ResponseOK.class, r);
    }

    {
      final var r =
        this.client.ask(
          new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));

      assertInstanceOf(ETCP0ResponseOK.class, r);
    }

    {
      final var r =
        this.client.ask(
          new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));

      assertInstanceOf(ETCP0ResponseOK.class, r);
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
      new ETCP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofMillis(10L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());

    this.client.send(new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));
    this.client.send(new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));
    this.client.send(new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));

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
  public void testConnectChat()
    throws Exception
  {
    final var parameters =
      new ETCP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofMillis(10L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());

    {
      final var r =
        this.client.ask(
          new ETCP0CommandHello(UUID.randomUUID(), "Chatting"));

      assertInstanceOf(ETCP0ResponseOK.class, r);
    }

    for (int index = 0; index < 5; ++index) {
      final var r =
        this.client.receive(Duration.ofSeconds(1L))
          .orElseThrow();
      assertInstanceOf(ETCP0CommandHello.class, r);
    }

    this.client.disconnect();

    assertEquals(
      List.of(
        "CONNECTING",
        "CONNECTION_SUCCEEDED",
        "CONNECTED",
        "DISCONNECTED"
      ),
      List.copyOf(this.clientStates)
    );
  }

  @Test
  public void testConnectFailure0()
    throws Exception
  {
    final var parameters =
      new ETCP0ConnectionParameters(
        ADDRESS,
        "someone",
        "wrong!",
        Duration.ofMillis(10L)
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
      new ETCP0ConnectionParameters(
        new InetSocketAddress(
          ADDRESS.getAddress(),
          9999
        ),
        "someone",
        "wrong!",
        Duration.ofMillis(10L)
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
      new ETCP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofMillis(10L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());

    assertThrows(ETCP0Exception.class, () -> {
      this.client.receive(Duration.ZERO);
    });
    assertThrows(ETCP0Exception.class, () -> {
      this.client.send(new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));
    });
    assertThrows(ETCP0Exception.class, () -> {
      this.client.ask(new ETCP0CommandHello(UUID.randomUUID(), "Hello!"));
    });
  }

  @Test
  public void testConnectConnect()
    throws Exception
  {
    final var parameters =
      new ETCP0ConnectionParameters(
        ADDRESS,
        "someone",
        "password",
        Duration.ofMillis(10L)
      );

    assertInstanceOf(HBStateDisconnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());
    this.client.connect(parameters);
    assertInstanceOf(HBStateConnected.class, this.client.stateNow());
  }
}
