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
import com.io7m.brooklime.api.BLStagingRepositoryClose;
import com.io7m.brooklime.api.BLStagingRepositoryCreate;
import com.io7m.brooklime.api.BLStagingRepositoryDrop;
import com.io7m.brooklime.api.BLStagingRepositoryRelease;
import com.io7m.brooklime.api.BLStagingRepositoryUpload;
import com.io7m.brooklime.api.BLStagingRepositoryUploadRequestParameters;
import com.io7m.brooklime.vanilla.internal.BLNexusParsers;
import com.io7m.brooklime.vanilla.internal.BLNexusRequests;
import com.io7m.brooklime.vanilla.internal.BLProgressCounter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.io7m.brooklime.tests.BLTestDirectories.createTempDirectory;
import static com.io7m.brooklime.tests.BLTestDirectories.resourceBytesOf;

public final class BLNexusRequestsTest
{
  private static ClientAndServer MOCK_SERVER;

  private HttpClient client;
  private URI serverAddress;
  private BLNexusParsers parsers;
  private Path directory;
  private BLNexusClientConfiguration basicConfiguration;
  private BLApplicationVersion appVersion;
  private ScheduledExecutorService executor;

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

    this.executor =
      Executors.newScheduledThreadPool(1);

    this.directory = createTempDirectory();
    this.parsers = new BLNexusParsers();
    this.client =
      HttpClient.newHttpClient();

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
        .setRetryDelay(Duration.ofSeconds(1L))
        .setRetryCount(3)
        .build();
  }

  @AfterEach
  public void tearDown()
  {
    this.executor.shutdown();
  }

  /**
   * Listing repositories fails on authentication errors.
   *
   * @throws Exception
   */

  @Test
  public void testRepositories401()
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
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

  /**
   * Listing repositories works.
   *
   * @throws Exception
   */

  @Test
  public void testRepositoriesOK()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
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

  /**
   * Creating a repository works in the absence of errors.
   *
   * @throws Exception
   */

  @Test
  public void testCreateRepositoryOK()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/profiles/6bfe53ee-d3ce-438d-a869-d501f01febb1/start")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
        .withBody(resourceBytesOf(
          this.directory, "createOK0.xml"))
    );

    final String id = requests.stagingRepositoryCreate(
      BLStagingRepositoryCreate.builder()
        .setDescription("Example repository")
        .build()
    );
    Assertions.assertEquals("r0", id);

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/profiles/6bfe53ee-d3ce-438d-a869-d501f01febb1/start"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Creating a repository fails if the server fails.
   *
   * @throws Exception
   */

  @Test
  public void testCreateRepositoryFails()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/profiles/6bfe53ee-d3ce-438d-a869-d501f01febb1/start")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(500))
    );

    Assertions.assertThrows(BLHTTPErrorException.class, () -> {
      requests.stagingRepositoryCreate(
        BLStagingRepositoryCreate.builder()
          .setDescription("Example repository")
          .build());
    });

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/profiles/6bfe53ee-d3ce-438d-a869-d501f01febb1/start"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Showing a repository works in the absence of errors.
   *
   * @throws Exception
   */

  @Test
  public void testShowRepositoryOK()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/repository/r0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
        .withBody(resourceBytesOf(
          this.directory, "stagingRepositoryCreated0.xml"))
    );

    final BLStagingProfileRepository repo =
      requests.stagingRepository("r0")
        .get();
    Assertions.assertEquals("r0", repo.repositoryId());

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/repository/r0"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Showing a repository fails if the server fails.
   *
   * @throws Exception
   */

  @Test
  public void testShowRepositoryFails()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/repository/r0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(500))
    );

    Assertions.assertThrows(BLHTTPErrorException.class, () -> {
      requests.stagingRepository("r0");
    });

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/repository/r0"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Showing a repository returns nothing if the server returns 404.
   *
   * @throws Exception
   */

  @Test
  public void testShowRepositoryMissing()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/repository/r0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(404))
    );

    final Optional<BLStagingProfileRepository> result =
      requests.stagingRepository("r0");
    Assertions.assertFalse(result.isPresent());

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/repository/r0"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Dropping a repository works in the absence of errors.
   *
   * @throws Exception
   */

  @Test
  public void testDropRepositoryOK()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/drop")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    requests.stagingRepositoryDrop(
      BLStagingRepositoryDrop.builder()
        .addStagingRepositories("x", "y", "z")
        .build()
    );

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/drop"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Dropping a repository fails if the server returns nonsense.
   *
   * @throws Exception
   */

  @Test
  public void testDropRepositoryNonsense()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/drop")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );

    Assertions.assertThrows(BLHTTPErrorException.class, () -> {
      requests.stagingRepositoryDrop(
        BLStagingRepositoryDrop.builder()
          .addStagingRepositories("x", "y", "z")
          .build()
      );
    });

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/drop"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Closing a repository works in the absence of errors.
   *
   * @throws Exception
   */

  @Test
  public void testCloseRepositoryOK()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    requests.stagingRepositoryClose(
      BLStagingRepositoryClose.builder()
        .addStagingRepositories("x", "y", "z")
        .build()
    );

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Closing a repository fails if the server returns nonsense.
   *
   * @throws Exception
   */

  @Test
  public void testCloseRepositoryNonsense()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );

    Assertions.assertThrows(BLHTTPErrorException.class, () -> {
      requests.stagingRepositoryClose(
        BLStagingRepositoryClose.builder()
          .addStagingRepositories("x", "y", "z")
          .build()
      );
    });

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Closing a repository fails if the server fails.
   *
   * @throws Exception
   */

  @Test
  public void testCloseRepositoryFails()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(500))
    );

    Assertions.assertThrows(BLHTTPErrorException.class, () -> {
      requests.stagingRepositoryClose(
        BLStagingRepositoryClose.builder()
          .addStagingRepositories("x", "y", "z")
          .build()
      );
    });

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/close"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Releasing a repository works in the absence of errors.
   *
   * @throws Exception
   */

  @Test
  public void testReleaseRepositoryOK()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/promote")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    requests.stagingRepositoryRelease(
      BLStagingRepositoryRelease.builder()
        .addStagingRepositories("x", "y", "z")
        .build()
    );

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/promote"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Releasing a repository fails if the server returns nonsense.
   *
   * @throws Exception
   */

  @Test
  public void testReleaseRepositoryNonsense()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/promote")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );

    Assertions.assertThrows(BLHTTPErrorException.class, () -> {
      requests.stagingRepositoryRelease(
        BLStagingRepositoryRelease.builder()
          .addStagingRepositories("x", "y", "z")
          .build()
      );
    });

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/promote"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Releasing a repository fails if the server fails.
   *
   * @throws Exception
   */

  @Test
  public void testReleaseRepositoryFails()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/promote")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(500))
    );

    Assertions.assertThrows(BLHTTPErrorException.class, () -> {
      requests.stagingRepositoryRelease(
        BLStagingRepositoryRelease.builder()
          .addStagingRepositories("x", "y", "z")
          .build()
      );
    });

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/bulk/promote"),
      VerificationTimes.exactly(1)
    );
  }

  /**
   * Creating an upload request works.
   *
   * @throws Exception
   */

  @Test
  public void testUploadRequestCreated()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    final Path subdir =
      this.directory.resolve("a").resolve("b").toAbsolutePath();
    Files.createDirectories(subdir);
    final Path file0 = subdir.resolve("file0.txt");
    Files.write(file0, "Hello".getBytes());
    final Path file1 = subdir.resolve("file1.txt");
    Files.write(file1, "Hello".getBytes());
    final Path file2 = subdir.resolve("file2.txt");
    Files.write(file2, "Hello".getBytes());

    final BLStagingRepositoryUpload request =
      requests.createUploadRequest(
        BLStagingRepositoryUploadRequestParameters.builder()
          .setRepositoryId("r0")
          .setBaseDirectory(this.directory)
          .setRetryDelay(Duration.ofSeconds(1L))
          .setRetryCount(3)
          .build()
      );

    final List<Path> files = request.files();
    Assertions.assertEquals(
      file0,
      this.directory.resolve(files.get(0)));
    Assertions.assertEquals(
      file1,
      this.directory.resolve(files.get(1)));
    Assertions.assertEquals(
      file2,
      this.directory.resolve(files.get(2)));
    Assertions.assertEquals(3, files.size());
  }

  /**
   * Executing an upload request works.
   *
   * @throws Exception
   */

  @Test
  public void testUploadWorks()
    throws Exception
  {
    final BLNexusRequests requests =
      new BLNexusRequests(
        this.executor,
        this.client,
        this.parsers,
        this.basicConfiguration
      );

    final Path subdir =
      this.directory.resolve("a").resolve("b").toAbsolutePath();
    Files.createDirectories(subdir);
    final Path file0 = subdir.resolve("file0.txt");
    Files.write(file0, "Hello".getBytes());
    final Path file1 = subdir.resolve("file1.txt");
    Files.write(file1, "Hello".getBytes());
    final Path file2 = subdir.resolve("file2.txt");
    Files.write(file2, "Hello".getBytes());

    final BLStagingRepositoryUpload request =
      requests.createUploadRequest(
        BLStagingRepositoryUploadRequestParameters.builder()
          .setRepositoryId("r0")
          .setBaseDirectory(this.directory)
          .setRetryDelay(Duration.ofMillis(100L))
          .setRetryCount(3)
          .build()
      );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withMethod("HEAD")
        .withPath("/service/local/staging/repository/r0")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(200))
    );

    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/deployByRepositoryId/r0/a/b/file0.txt")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/deployByRepositoryId/r0/a/b/file1.txt")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );
    MOCK_SERVER.when(
      HttpRequest.request()
        .withPath(
          "/service/local/staging/deployByRepositoryId/r0/a/b/file2.txt")
    ).respond(
      HttpResponse.response()
        .withStatusCode(Integer.valueOf(201))
    );

    requests.upload(
      new BLProgressCounter(Clock.systemUTC(), event -> {
      }),
      request
    );

    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/deployByRepositoryId/r0/a/b/file0.txt"),
      VerificationTimes.exactly(1)
    );
    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/deployByRepositoryId/r0/a/b/file1.txt"),
      VerificationTimes.exactly(1)
    );
    MOCK_SERVER.verify(
      HttpRequest.request()
        .withPath("/service/local/staging/deployByRepositoryId/r0/a/b/file2.txt"),
      VerificationTimes.exactly(1)
    );
  }
}
