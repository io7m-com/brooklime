/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.brooklime.tests;

import com.io7m.brooklime.api.BLApplicationVersion;
import com.io7m.brooklime.api.BLHTTPFailureException;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLProgressEventType;
import com.io7m.brooklime.vanilla.internal.BLNexusParsers;
import com.io7m.brooklime.vanilla.internal.BLProgressCounter;
import com.io7m.brooklime.vanilla.internal.BLRetryingUploader;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;

import static com.io7m.brooklime.tests.BLTestDirectories.createTempDirectory;

public final class BLRetryingUploaderTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLRetryingUploaderTest.class);

  private static ClientAndServer MOCK_SERVER;

  private CloseableHttpClient client;
  private URI serverAddress;
  private BLNexusParsers parsers;
  private Path directory;
  private BLNexusClientConfiguration basicConfiguration;
  private BLApplicationVersion appVersion;
  private ArrayList<BLProgressEventType> events;
  private BLProgressCounter progressCounter;
  private Path helloFile;

  @BeforeAll
  public static void startServer()
  {
    MOCK_SERVER = ClientAndServer.startClientAndServer();
  }

  @AfterAll
  public static void stopServer()
  {
    MOCK_SERVER.stop();
  }

  @BeforeEach
  public void testSetup()
    throws IOException
  {
    MOCK_SERVER.reset();

    this.directory = createTempDirectory();
    this.parsers = new BLNexusParsers();
    this.client = HttpClients.createDefault();
    final InetSocketAddress remoteAddress = MOCK_SERVER.remoteAddress();
    this.serverAddress =
      URI.create(
        String.format(
          "http://%s:%d/",
          remoteAddress.getAddress().getHostAddress(),
          Integer.valueOf(remoteAddress.getPort())
        )
      );

    this.appVersion =
      BLApplicationVersion.builder()
        .setApplicationName("com.io7m.brooklime.tests")
        .setApplicationVersion("0.0.1")
        .build();

    this.basicConfiguration =
      BLNexusClientConfiguration.builder()
        .setUserName("user")
        .setPassword("password")
        .setApplicationVersion(this.appVersion)
        .setStagingProfileId("6bfe53ee-d3ce-438d-a869-d501f01febb1")
        .setBaseURI(this.serverAddress)
        .setRetryCount(3)
        .setRetryDelay(Duration.ofSeconds(1L))
        .build();

    this.events =
      new ArrayList<>();
    this.progressCounter =
      new BLProgressCounter(Clock.systemUTC(), this::logEvent);

    this.helloFile =
      Files.write(
        this.directory.resolve("file.txt"),
        "Hello.".getBytes()
      );
  }

  /*
   * If the every request succeeds, the uploader succeeds.
   */

  @Test
  public void testUploadSimple()
    throws Exception
  {
    final BLRetryingUploader uploader =
      new BLRetryingUploader(
        this.client,
        this.serverAddress,
        this.serverAddress,
        this.helloFile,
        1,
        1,
        Duration.of(100L, ChronoUnit.MILLIS),
        10,
        this.progressCounter
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/")
        .withMethod("PUT")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );

    uploader.execute();

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD"),
      VerificationTimes.exactly(1)
    );
    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/")
        .withMethod("PUT"),
      VerificationTimes.exactly(1)
    );
  }

  /*
   * If the initial HEAD request fails, the uploader retries.
   */

  @Test
  public void testUploadFailFirstRetry()
    throws Exception
  {
    final BLRetryingUploader uploader =
      new BLRetryingUploader(
        this.client,
        this.serverAddress,
        this.serverAddress,
        this.helloFile,
        1,
        1,
        Duration.of(100L, ChronoUnit.MILLIS),
        10,
        this.progressCounter
      );

    final LinkedList<HttpResponse> headResponses = new LinkedList<>();
    headResponses.add(HttpResponse.response().withStatusCode(Integer.valueOf(500)));
    headResponses.add(HttpResponse.response().withStatusCode(Integer.valueOf(200)));

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD")
    ).respond(
      httpRequest -> headResponses.remove()
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/")
        .withMethod("PUT")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );

    uploader.execute();

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD"),
      VerificationTimes.exactly(2));

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/")
        .withMethod("PUT"),
      VerificationTimes.exactly(1));
  }

  /*
   * If the initial PUT request fails, the uploader retries.
   */

  @Test
  public void testUploadFailSecondRetry()
    throws Exception
  {
    final BLRetryingUploader uploader =
      new BLRetryingUploader(
        this.client,
        this.serverAddress,
        this.serverAddress,
        this.helloFile,
        1,
        1,
        Duration.of(100L, ChronoUnit.MILLIS),
        10,
        this.progressCounter
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );

    final LinkedList<HttpResponse> putResponses = new LinkedList<>();
    putResponses.add(HttpResponse.response().withStatusCode(Integer.valueOf(500)));
    putResponses.add(HttpResponse.response().withStatusCode(Integer.valueOf(200)));

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/")
        .withMethod("PUT")
    ).respond(
      httpRequest -> putResponses.remove()
    );

    uploader.execute();

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD"),
      VerificationTimes.exactly(2));

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/")
        .withMethod("PUT"),
      VerificationTimes.exactly(2));
  }

  /*
   * If the uploader reaches the maximum number of retries, it fails.
   */

  @Test
  public void testUploadFailMaximum()
    throws Exception
  {
    final BLRetryingUploader uploader =
      new BLRetryingUploader(
        this.client,
        this.serverAddress,
        this.serverAddress,
        this.helloFile,
        1,
        1,
        Duration.of(100L, ChronoUnit.MILLIS),
        3,
        this.progressCounter
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD")
    ).respond(
      HttpResponse.response().withStatusCode(Integer.valueOf(500))
    );

    Assertions.assertThrows(BLHTTPFailureException.class, uploader::execute);

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/")
        .withMethod("HEAD"),
      VerificationTimes.exactly(3));
  }

  private <T extends BLProgressEventType> T take(
    final Class<T> clazz)
  {
    return clazz.cast(this.events.remove(0));
  }

  private void logEvent(
    final BLProgressEventType event)
  {
    LOG.debug("event: {}", event);
    this.events.add(event);
  }
}
