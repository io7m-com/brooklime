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
import com.io7m.brooklime.api.BLHTTPErrorException;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import com.io7m.brooklime.vanilla.internal.BLNexusParsers;
import com.io7m.brooklime.vanilla.internal.BLNexusRequests;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static com.io7m.brooklime.tests.BLTestDirectories.createTempDirectory;
import static com.io7m.brooklime.tests.BLTestDirectories.resourceBytesOf;

public final class BLNexusRequestsTest
{
  private static ClientAndServer MOCK_SERVER;

  private CloseableHttpClient client;
  private URI serverAddress;
  private BLNexusParsers parsers;
  private Path directory;
  private BLNexusClientConfiguration basicConfiguration;
  private BLApplicationVersion appVersion;

  @BeforeAll
  public static void startProxy()
  {
    MOCK_SERVER = ClientAndServer.startClientAndServer();
  }

  @AfterAll
  public static void stopProxy()
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
        .build();
  }

  @Test
  public void testRepositories401()
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.client,
        this.appVersion,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/profile_repositories")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(401))
    );

    final BLHTTPErrorException ex =
      Assertions.assertThrows(
        BLHTTPErrorException.class,
        requests::stagingRepositories
      );

    Assertions.assertEquals(401, ex.statusCode());

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/profile_repositories"),
      VerificationTimes.exactly(1)
    );
  }

  @Test
  public void testRepositoriesOK()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.client,
        this.appVersion,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/profile_repositories")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
        .withBody(resourceBytesOf(this.directory, "stagingRepositories0.xml"))
    );

    final List<BLStagingProfileRepository> repositories =
      requests.stagingRepositories();

    Assertions.assertEquals(3, repositories.size());
    Assertions.assertEquals("r0", repositories.get(0).repositoryId());
    Assertions.assertEquals("r1", repositories.get(1).repositoryId());
    Assertions.assertEquals("r2", repositories.get(2).repositoryId());

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/profile_repositories"),
      VerificationTimes.exactly(1)
    );
  }
}
