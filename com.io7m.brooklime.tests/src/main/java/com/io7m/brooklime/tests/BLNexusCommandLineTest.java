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
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.cmdline.MainExitless;
import com.io7m.brooklime.vanilla.internal.BLNexusParsers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.io7m.brooklime.tests.BLTestDirectories.createTempDirectory;
import static com.io7m.brooklime.tests.BLTestDirectories.resourceBytesOf;

public final class BLNexusCommandLineTest
{
  private static ClientAndServer MOCK_SERVER;

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
    final InetSocketAddress remoteAddress = MOCK_SERVER.remoteAddress();
    this.serverAddress =
      URI.create(
        String.format(
          "http://%s:%d/",
          remoteAddress.getAddress().getHostAddress(),
          Integer.valueOf(remoteAddress.getPort())
        )
      );
  }

  /**
   * Passing no arguments is an error.
   */

  @Test
  public void testNoArguments()
  {
    Assertions.assertThrows(IOException.class, () -> {
      MainExitless.main(new String[]{

      });
    });
  }

  /**
   * Listing staging repositories works if the server returns the right data.
   */

  @Test
  public void testListStagingRepositories()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/profile_repositories")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
        .withBody("<stagingRepositories><data></data></stagingRepositories>")
    );

    MainExitless.main(new String[]{
      "list",
      "--user",
      "user",
      "--password",
      "pass",
      "--stagingProfileId",
      "88536b02-fb30-4ee3-9831-0c5b290bd913",
      "--verbose",
      "trace",
      "--baseURI",
      this.serverAddress.toString()
    });
  }

  /**
   * Closing staging repositories works if the server returns the right data.
   */

  @Test
  public void testCloseStagingRepository()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/repository/example-0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
        .withBody(resourceBytesOf(
          this.directory,
          "stagingRepositoryClosed0.xml"))
    );

    MainExitless.main(new String[]{
      "close",
      "--user",
      "user",
      "--password",
      "pass",
      "--stagingProfileId",
      "88536b02-fb30-4ee3-9831-0c5b290bd913",
      "--verbose",
      "trace",
      "--repository",
      "example-0",
      "--baseURI",
      this.serverAddress.toString()
    });
  }

  /**
   * Closing staging repositories fails if the repository vanishes.
   */

  @Test
  public void testCloseStagingRepositoryVanished()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/repository/example-0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(404))
    );

    Assertions.assertThrows(IOException.class, () -> {
      MainExitless.main(new String[]{
        "close",
        "--user",
        "user",
        "--password",
        "pass",
        "--stagingProfileId",
        "88536b02-fb30-4ee3-9831-0c5b290bd913",
        "--verbose",
        "trace",
        "--repository",
        "example-0",
        "--baseURI",
        this.serverAddress.toString()
      });
    });
  }

  /**
   * Dropping staging repositories succeeds if the repository vanishes.
   */

  @Test
  public void testDropStagingRepository()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/drop")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/repository/example-0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(404))
    );

    MainExitless.main(new String[]{
      "drop",
      "--user",
      "user",
      "--password",
      "pass",
      "--stagingProfileId",
      "88536b02-fb30-4ee3-9831-0c5b290bd913",
      "--verbose",
      "trace",
      "--repository",
      "example-0",
      "--baseURI",
      this.serverAddress.toString()
    });
  }

  /**
   * Creating staging repositories works if the server returns the right data.
   */

  @Test
  public void testCreateStagingRepository()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/profiles/88536b02-fb30-4ee3-9831-0c5b290bd913/start")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
        .withBody(
          "<promoteResponse><data><stagedRepositoryId>r0</stagedRepositoryId></data></promoteResponse>")
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/repository/r0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
        .withBody(resourceBytesOf(
          this.directory,
          "stagingRepositoryCreated0.xml"))
    );

    MainExitless.main(new String[]{
      "create",
      "--user",
      "user",
      "--password",
      "pass",
      "--stagingProfileId",
      "88536b02-fb30-4ee3-9831-0c5b290bd913",
      "--verbose",
      "trace",
      "--description",
      "An example repository.",
      "--baseURI",
      this.serverAddress.toString()
    });
  }

  /**
   * Creating staging repositories works if the server returns the right data.
   */

  @Test
  public void testCreateStagingRepositoryOutput()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/profiles/88536b02-fb30-4ee3-9831-0c5b290bd913/start")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
        .withBody(
          "<promoteResponse><data><stagedRepositoryId>r0</stagedRepositoryId></data></promoteResponse>")
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/repository/r0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
        .withBody(resourceBytesOf(
          this.directory,
          "stagingRepositoryCreated0.xml"))
    );

    final Path outputFile =
      this.directory.resolve("repository.txt");

    MainExitless.main(new String[]{
      "create",
      "--user",
      "user",
      "--password",
      "pass",
      "--stagingProfileId",
      "88536b02-fb30-4ee3-9831-0c5b290bd913",
      "--verbose",
      "trace",
      "--description",
      "An example repository.",
      "--baseURI",
      this.serverAddress.toString(),
      "--outputFile",
      outputFile.toString()
    });

    Assertions.assertEquals(
      "r0",
      new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8).trim()
    );
  }

  /**
   * Showing staging repositories works if the server returns the right data.
   */

  @Test
  public void testShowStagingRepository()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/repository/r0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
        .withBody(resourceBytesOf(
          this.directory,
          "stagingRepositoryCreated0.xml"))
    );

    MainExitless.main(new String[]{
      "show",
      "--user",
      "user",
      "--password",
      "pass",
      "--stagingProfileId",
      "88536b02-fb30-4ee3-9831-0c5b290bd913",
      "--verbose",
      "trace",
      "--repository",
      "r0",
      "--baseURI",
      this.serverAddress.toString()
    });
  }

  /**
   * Releasing staging repositories works if the server returns the right data.
   */

  @Test
  public void testReleaseStagingRepository()
    throws Exception
  {
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/promote")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/repository/example-0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(404))
    );

    MainExitless.main(new String[]{
      "release",
      "--user",
      "user",
      "--password",
      "pass",
      "--stagingProfileId",
      "88536b02-fb30-4ee3-9831-0c5b290bd913",
      "--verbose",
      "trace",
      "--repository",
      "example-0",
      "--baseURI",
      this.serverAddress.toString()
    });
  }

  /**
   * Showing the version works.
   */

  @Test
  public void testVersion()
    throws Exception
  {
    MainExitless.main(new String[]{
      "version"
    });
  }

  /**
   * All of the supported log levels work.
   */

  @Test
  public void testVersionLogLevels()
    throws Exception
  {
    final List<String> levels =
      Stream.of(
        "trace",
        "debug",
        "info",
        "error",
        "warn"
      ).collect(Collectors.toList());

    for (final String level : levels) {
      MainExitless.main(new String[]{
        "version",
        "--verbose",
        level
      });
    }
  }

  /**
   * All of the supported log levels work.
   */

  @Test
  public void testVersionLogLevelInvalid()
    throws Exception
  {
    Assertions.assertThrows(IOException.class, () -> {
      MainExitless.main(new String[]{
        "version",
        "--verbose",
        "invalid"
      });
    });
  }
}
