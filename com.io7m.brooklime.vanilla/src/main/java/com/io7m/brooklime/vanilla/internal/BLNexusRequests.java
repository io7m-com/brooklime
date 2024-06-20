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

package com.io7m.brooklime.vanilla.internal;

import com.io7m.brooklime.api.BLException;
import com.io7m.brooklime.api.BLHTTPErrorException;
import com.io7m.brooklime.api.BLHTTPFailureException;
import com.io7m.brooklime.api.BLNexusClientConfiguration;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import com.io7m.brooklime.api.BLStagingRepositoryBulkRequestType;
import com.io7m.brooklime.api.BLStagingRepositoryClose;
import com.io7m.brooklime.api.BLStagingRepositoryCreate;
import com.io7m.brooklime.api.BLStagingRepositoryDrop;
import com.io7m.brooklime.api.BLStagingRepositoryRelease;
import com.io7m.brooklime.api.BLStagingRepositoryUpload;
import com.io7m.brooklime.api.BLStagingRepositoryUploadRequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Nexus request provider.
 */

public final class BLNexusRequests
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLNexusRequests.class);
  private static final Pattern TRAILING_SLASHES =
    Pattern.compile("/+$");

  private final ScheduledExecutorService executor;
  private final HttpClient client;
  private final BLNexusParsers parsers;
  private final BLNexusClientConfiguration configuration;
  private final XMLOutputFactory outputs;

  /**
   * A Nexus request provider.
   *
   * @param inExecutor      An executor service
   * @param inClient        An HTTP client
   * @param inNexusParsers  A provider of Nexus parsers
   * @param inConfiguration The client configuration
   */

  public BLNexusRequests(
    final ScheduledExecutorService inExecutor,
    final HttpClient inClient,
    final BLNexusParsers inNexusParsers,
    final BLNexusClientConfiguration inConfiguration)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "inExecutor");
    this.client =
      Objects.requireNonNull(inClient, "inClient");
    this.parsers =
      Objects.requireNonNull(inNexusParsers, "inNexusParsers");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.outputs =
      XMLOutputFactory.newFactory();
  }

  private static String scrubTrailingSlashes(
    final String baseURI)
  {
    return TRAILING_SLASHES.matcher(baseURI).replaceAll("");
  }

  private static String translateFileToURIPath(
    final Path file)
  {
    final var filesystem = file.getFileSystem();

    final Path relative;
    if (file.isAbsolute()) {
      final var root = file.getRoot();
      relative = root.relativize(file);
    } else {
      relative = file;
    }

    return relative.toString()
      .replace(filesystem.getSeparator(), "/");
  }

  /**
   * Request a list of staging repositories from the server.
   *
   * @return A list of staging repositories
   *
   * @throws BLException On errors
   */

  public List<BLStagingProfileRepository> stagingRepositories()
    throws BLException
  {
    final var baseURI = this.configuration.baseURI().toString();
    final var uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/profile_repositories");

    try {
      final var uri =
        URI.create(uriBuilder.toString());

      final var httpGet =
        HttpRequest.newBuilder(uri)
          .GET()
          .build();

      final var response =
        this.client.send(httpGet, BodyHandlers.ofInputStream());

      final var status = response.statusCode();
      if (status >= 400) {
        throw new BLHTTPErrorException(
          status,
          errorMessageOf(status, response),
          this.parsers.parseErrorsIfPresent(
            contentTypeOf(response),
            uri,
            response.body())
        );
      }

      return this.parsers.parseRepositories(uri, response.body());
    } catch (final BLHTTPErrorException e) {
      throw e;
    } catch (final Exception e) {
      throw new BLHTTPFailureException(e);
    }
  }

  private static String errorMessageOf(
    final int status,
    final HttpResponse<?> response)
  {
    return "Error: %d".formatted(Integer.valueOf(status));
  }

  /**
   * Request a staging repository from the server.
   *
   * @param repositoryId The repository ID
   *
   * @return A staging repository
   *
   * @throws BLException On errors
   */

  public Optional<BLStagingProfileRepository> stagingRepository(
    final String repositoryId)
    throws BLException
  {
    final var baseURI = this.configuration.baseURI().toString();
    final var uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/repository/");
    uriBuilder.append(repositoryId);

    try {
      final var uri =
        URI.create(uriBuilder.toString());

      final var httpGet =
        HttpRequest.newBuilder(uri)
          .GET()
          .build();

      final var response =
        this.client.send(httpGet, BodyHandlers.ofInputStream());

      final var status = response.statusCode();
      if (status == 404) {
        return Optional.empty();
      }

      if (status >= 400) {
        throw new BLHTTPErrorException(
          status,
          errorMessageOf(status, response),
          this.parsers.parseErrorsIfPresent(
            contentTypeOf(response),
            uri,
            response.body())
        );
      }

      return Optional.of(this.parsers.parseRepository(uri, response.body()));
    } catch (final BLHTTPErrorException e) {
      throw e;
    } catch (final Exception e) {
      throw new BLHTTPFailureException(e);
    }
  }

  private byte[] stagingRepositoryCreateToXML(
    final BLStagingRepositoryCreate create)
    throws IOException
  {
    try (var stream = new ByteArrayOutputStream()) {
      final var output =
        this.outputs.createXMLStreamWriter(stream);
      output.writeStartElement("promoteRequest");
      output.writeStartElement("data");
      output.writeStartElement("description");
      output.writeCharacters(create.description());
      output.writeEndElement();
      output.writeEndElement();
      output.writeEndElement();
      output.flush();
      return stream.toByteArray();
    } catch (final XMLStreamException e) {
      throw new IOException(e);
    }
  }

  private byte[] stagingRepositoryBulkRequestToXML(
    final BLStagingRepositoryBulkRequestType request)
    throws IOException
  {
    try (var stream = new ByteArrayOutputStream()) {
      final var output =
        this.outputs.createXMLStreamWriter(stream);
      output.writeStartElement("stagingActionRequest");
      output.writeStartElement("data");
      output.writeStartElement("stagedRepositoryIds");

      for (final var id : request.stagingRepositories()) {
        output.writeStartElement("string");
        output.writeCharacters(id);
        output.writeEndElement();
      }

      output.writeEndElement();
      output.writeEndElement();
      output.writeEndElement();
      output.flush();
      return stream.toByteArray();
    } catch (final XMLStreamException e) {
      throw new IOException(e);
    }
  }

  private byte[] stagingRepositoryReleaseToXML(
    final BLStagingRepositoryRelease request)
    throws IOException
  {
    try (var stream = new ByteArrayOutputStream()) {
      final var output =
        this.outputs.createXMLStreamWriter(stream);
      output.writeStartElement("stagingActionRequest");
      output.writeStartElement("data");

      output.writeStartElement("stagedRepositoryIds");
      for (final var id : request.stagingRepositories()) {
        output.writeStartElement("string");
        output.writeCharacters(id);
        output.writeEndElement();
      }
      output.writeEndElement();

      output.writeStartElement("autoDropAfterRelease");
      output.writeCharacters("true");
      output.writeEndElement();

      output.writeEndElement();
      output.writeEndElement();
      output.flush();
      return stream.toByteArray();
    } catch (final XMLStreamException e) {
      throw new IOException(e);
    }
  }

  /**
   * Create a staging repository on the server.
   *
   * @param create The repository creation info
   *
   * @return A staging repository ID
   *
   * @throws BLException On errors
   */

  public String stagingRepositoryCreate(
    final BLStagingRepositoryCreate create)
    throws BLException
  {
    final var baseURI = this.configuration.baseURI().toString();
    final var uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/profiles/");
    uriBuilder.append(this.configuration.stagingProfileId());
    uriBuilder.append("/start");

    try {
      final var uri =
        URI.create(uriBuilder.toString());

      final BodyPublisher body;
      try {
        body = BodyPublishers.ofByteArray(
          this.stagingRepositoryCreateToXML(create)
        );
      } catch (final IOException e) {
        throw new BLException(e);
      }

      final var httpPost =
        HttpRequest.newBuilder(uri)
          .POST(body)
          .header("Content-Type", "application/xml")
          .build();

      final var response =
        this.client.send(httpPost, BodyHandlers.ofInputStream());

      final var status = response.statusCode();
      if (status >= 400) {
        throw new BLHTTPErrorException(
          status,
          errorMessageOf(status, response),
          this.parsers.parseErrorsIfPresent(
            contentTypeOf(response),
            uri,
            response.body())
        );
      }

      return this.parsers.parseStagingRepositoryCreate(uri, response.body());
    } catch (final BLHTTPErrorException e) {
      throw e;
    } catch (final Exception e) {
      throw new BLHTTPFailureException(e);
    }
  }

  /**
   * Drop a staging repository on the server.
   *
   * @param drop The repository info
   *
   * @throws BLException On errors
   * @throws IOException On errors
   */

  public void stagingRepositoryDrop(
    final BLStagingRepositoryDrop drop)
    throws BLException, IOException
  {
    final var baseURI = this.configuration.baseURI().toString();
    final var uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/bulk/drop");

    final var targetURI = uriBuilder.toString();
    this.executeBulkRequest(
      targetURI, this.stagingRepositoryBulkRequestToXML(drop));
  }

  /**
   * Close a staging repository on the server.
   *
   * @param close The repository info
   *
   * @throws BLException On errors
   * @throws IOException On errors
   */

  public void stagingRepositoryClose(
    final BLStagingRepositoryClose close)
    throws BLException, IOException
  {
    final var baseURI = this.configuration.baseURI().toString();
    final var uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/bulk/close");

    final var targetURI = uriBuilder.toString();
    this.executeBulkRequest(
      targetURI, this.stagingRepositoryBulkRequestToXML(close));
  }

  /**
   * Release a staging repository on the server.
   *
   * @param release The repository info
   *
   * @throws BLException On errors
   * @throws IOException On errors
   */

  public void stagingRepositoryRelease(
    final BLStagingRepositoryRelease release)
    throws BLException, IOException
  {
    final var baseURI = this.configuration.baseURI().toString();
    final var uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/bulk/promote");

    final var targetURI = uriBuilder.toString();
    this.executeBulkRequest(
      targetURI, this.stagingRepositoryReleaseToXML(release));
  }

  private void executeBulkRequest(
    final String targetURI,
    final byte[] postData)
    throws BLException
  {
    try {
      final var uri =
        URI.create(targetURI);

      final var httpPost =
        HttpRequest.newBuilder(uri)
          .POST(BodyPublishers.ofByteArray(postData))
          .header("Content-Type", "application/xml")
          .build();

      final var response =
        this.client.send(httpPost, BodyHandlers.ofInputStream());

      final var status = response.statusCode();
      if (status >= 400) {
        throw new BLHTTPErrorException(
          status,
          errorMessageOf(status, response),
          this.parsers.parseErrorsIfPresent(
            contentTypeOf(response),
            uri,
            response.body())
        );
      }

      if (status != 201) {
        throw new BLHTTPErrorException(
          status,
          String.format(
            "Expected server to return 201 Created, but received: %d",
            Integer.valueOf(status)
          ),
          this.parsers.parseErrorsIfPresent(
            contentTypeOf(response),
            uri,
            response.body())
        );
      }
    } catch (final BLHTTPErrorException e) {
      throw e;
    } catch (final Exception e) {
      throw new BLHTTPFailureException(e);
    }
  }

  private static String contentTypeOf(
    final HttpResponse<InputStream> response)
  {
    return response.headers()
      .firstValue("Content-Type")
      .orElse("application/octet-stream");
  }

  /**
   * Create an upload request for the server.
   *
   * @param parameters The upload info
   *
   * @return An upload
   *
   * @throws BLException On errors
   */

  public BLStagingRepositoryUpload createUploadRequest(
    final BLStagingRepositoryUploadRequestParameters parameters)
    throws BLException
  {
    try {
      final var absoluteBase =
        parameters.baseDirectory().toAbsolutePath();

      final List<Path> files;
      try (var pathStream = Files.walk(absoluteBase)) {
        files = pathStream.filter(Files::isRegularFile)
          .map(absoluteBase::relativize)
          .sorted()
          .collect(Collectors.toList());
      }

      for (final var file : files) {
        LOG.debug("upload {} -> /{}", file.toAbsolutePath(), file);
      }

      return BLStagingRepositoryUpload.builder()
        .setBaseDirectory(absoluteBase)
        .setFiles(files)
        .setRepositoryId(parameters.repositoryId())
        .setRetryCount(parameters.retryCount())
        .setRetryDelay(parameters.retryDelay())
        .build();
    } catch (final IOException e) {
      throw new BLException(e);
    }
  }

  /**
   * Execute an upload request for the server.
   *
   * @param counter The progress counter
   * @param upload  The upload
   *
   * @throws BLException On errors
   */

  public void upload(
    final BLProgressCounter counter,
    final BLStagingRepositoryUpload upload)
    throws BLException
  {
    final var files = upload.files();
    for (int fileIndex = 0, fileMax = files.size(); fileIndex < fileMax; ++fileIndex) {
      final var file = files.get(fileIndex);

      final var actual =
        upload.baseDirectory().resolve(file).toAbsolutePath();

      final var baseURI = this.configuration.baseURI().toString();
      final var uriBuilder = new StringBuilder(128);
      uriBuilder.append(scrubTrailingSlashes(baseURI));
      uriBuilder.append("/service/local/staging/deployByRepositoryId/");
      uriBuilder.append(upload.repositoryId());
      uriBuilder.append("/");
      uriBuilder.append(translateFileToURIPath(file));
      final var targetURI = URI.create(uriBuilder.toString());

      final var serviceUriBuilder = new StringBuilder(128);
      serviceUriBuilder.append(scrubTrailingSlashes(baseURI));
      serviceUriBuilder.append("/service/local/staging/repository/");
      serviceUriBuilder.append(upload.repositoryId());
      final var serviceURI = URI.create(serviceUriBuilder.toString());

      final var uploader =
        new BLRetryingUploader(
          this.executor,
          this.client,
          serviceURI,
          targetURI,
          actual,
          fileIndex,
          fileMax,
          upload.retryDelay(),
          upload.retryCount(),
          counter
        );

      uploader.execute();
    }
  }
}
