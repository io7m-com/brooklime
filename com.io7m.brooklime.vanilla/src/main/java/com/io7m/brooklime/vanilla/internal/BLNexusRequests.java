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
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.hc.core5.http.ContentType.APPLICATION_XML;
import static org.apache.hc.core5.http.HttpStatus.SC_CLIENT_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static org.apache.hc.core5.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;

public final class BLNexusRequests
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLNexusRequests.class);
  private static final Pattern TRAILING_SLASHES =
    Pattern.compile("/+$");

  private final CloseableHttpClient client;
  private final BLNexusParsers parsers;
  private final BLNexusClientConfiguration configuration;
  private final XMLOutputFactory outputs;

  public BLNexusRequests(
    final CloseableHttpClient inClient,
    final BLNexusParsers inNexusParsers,
    final BLNexusClientConfiguration inConfiguration)
  {
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

  public List<BLStagingProfileRepository> stagingRepositories()
    throws BLException
  {
    final String baseURI = this.configuration.baseURI().toString();
    final StringBuilder uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/profile_repositories");

    final HttpUriRequest httpGet = new HttpGet(uriBuilder.toString());
    try (CloseableHttpResponse response = this.client.execute(httpGet)) {
      final int status = response.getCode();
      if (status >= SC_CLIENT_ERROR && status <= SC_INTERNAL_SERVER_ERROR) {
        throw new BLHTTPErrorException(status, response.getReasonPhrase());
      }

      return this.parsers.parseRepositories(
        httpGet.getUri(),
        response.getEntity().getContent()
      );
    } catch (final IOException | URISyntaxException e) {
      throw new BLHTTPFailureException(e);
    }
  }

  public Optional<BLStagingProfileRepository> stagingRepository(
    final String repositoryId)
    throws BLException
  {
    final String baseURI = this.configuration.baseURI().toString();
    final StringBuilder uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/repository/");
    uriBuilder.append(repositoryId);

    final HttpUriRequest httpGet = new HttpGet(uriBuilder.toString());
    try (CloseableHttpResponse response = this.client.execute(httpGet)) {
      final int status = response.getCode();
      if (status == SC_NOT_FOUND) {
        return Optional.empty();
      }

      if (status >= SC_CLIENT_ERROR && status <= SC_INTERNAL_SERVER_ERROR) {
        throw new BLHTTPErrorException(status, response.getReasonPhrase());
      }

      return Optional.of(
        this.parsers.parseRepository(
          httpGet.getUri(),
          response.getEntity().getContent()
        )
      );
    } catch (final IOException | URISyntaxException e) {
      throw new BLHTTPFailureException(e);
    }
  }

  private byte[] stagingRepositoryCreateToXML(
    final BLStagingRepositoryCreate create)
    throws IOException
  {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      final XMLStreamWriter output =
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
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      final XMLStreamWriter output =
        this.outputs.createXMLStreamWriter(stream);
      output.writeStartElement("stagingActionRequest");
      output.writeStartElement("data");
      output.writeStartElement("stagedRepositoryIds");

      for (final String id : request.stagingRepositories()) {
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
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      final XMLStreamWriter output =
        this.outputs.createXMLStreamWriter(stream);
      output.writeStartElement("stagingActionRequest");
      output.writeStartElement("data");

      output.writeStartElement("stagedRepositoryIds");
      for (final String id : request.stagingRepositories()) {
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

  public String stagingRepositoryCreate(
    final BLStagingRepositoryCreate create)
    throws BLException
  {
    final String baseURI = this.configuration.baseURI().toString();
    final StringBuilder uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/profiles/");
    uriBuilder.append(this.configuration.stagingProfileId());
    uriBuilder.append("/start");

    final HttpPost post = new HttpPost(uriBuilder.toString());

    try {
      post.setEntity(
        new ByteArrayEntity(
          this.stagingRepositoryCreateToXML(create), APPLICATION_XML
        )
      );
    } catch (final IOException e) {
      throw new BLException(e);
    }

    try (CloseableHttpResponse response = this.client.execute(post)) {
      final int status = response.getCode();
      if (status >= SC_CLIENT_ERROR && status <= SC_INTERNAL_SERVER_ERROR) {
        throw new BLHTTPErrorException(status, response.getReasonPhrase());
      }

      return this.parsers.parseStagingRepositoryCreate(
        post.getUri(),
        response.getEntity().getContent()
      );
    } catch (final IOException | URISyntaxException e) {
      throw new BLHTTPFailureException(e);
    }
  }

  public void stagingRepositoryDrop(
    final BLStagingRepositoryDrop drop)
    throws BLException, IOException
  {
    final String baseURI = this.configuration.baseURI().toString();
    final StringBuilder uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/bulk/drop");

    final String targetURI = uriBuilder.toString();
    this.executeBulkRequest(
      targetURI, this.stagingRepositoryBulkRequestToXML(drop));
  }

  public void stagingRepositoryClose(
    final BLStagingRepositoryClose close)
    throws BLException, IOException
  {
    final String baseURI = this.configuration.baseURI().toString();
    final StringBuilder uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/bulk/close");

    final String targetURI = uriBuilder.toString();
    this.executeBulkRequest(
      targetURI, this.stagingRepositoryBulkRequestToXML(close));
  }

  public void stagingRepositoryRelease(
    final BLStagingRepositoryRelease release)
    throws BLException, IOException
  {
    final String baseURI = this.configuration.baseURI().toString();
    final StringBuilder uriBuilder = new StringBuilder();
    uriBuilder.append(scrubTrailingSlashes(baseURI));
    uriBuilder.append("/service/local/staging/bulk/promote");

    final String targetURI = uriBuilder.toString();
    this.executeBulkRequest(
      targetURI, this.stagingRepositoryReleaseToXML(release));
  }

  private void executeBulkRequest(
    final String targetURI,
    final byte[] postData)
    throws BLException
  {
    final HttpPost post = new HttpPost(targetURI);
    post.setEntity(new ByteArrayEntity(postData, APPLICATION_XML));
    try (CloseableHttpResponse response = this.client.execute(post)) {
      final int status = response.getCode();
      if (status >= SC_CLIENT_ERROR && status <= SC_INTERNAL_SERVER_ERROR) {
        throw new BLHTTPErrorException(status, response.getReasonPhrase());
      }

      if (status != SC_CREATED) {
        throw new BLHTTPErrorException(
          status,
          String.format(
            "Expected server to return 201 Created, but received: %d",
            Integer.valueOf(status))
        );
      }
    } catch (final IOException e) {
      throw new BLHTTPFailureException(e);
    }
  }


  public BLStagingRepositoryUpload createUploadRequest(
    final BLStagingRepositoryUploadRequestParameters parameters)
    throws BLException
  {
    try {
      final Path absoluteBase =
        parameters.baseDirectory().toAbsolutePath();

      final List<Path> files;
      try (Stream<Path> pathStream = Files.walk(absoluteBase)) {
        files = pathStream.filter(path -> Files.isRegularFile(path))
          .map(absoluteBase::relativize)
          .sorted()
          .collect(Collectors.toList());
      }

      for (final Path file : files) {
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

  public void upload(
    final BLProgressCounter counter,
    final BLStagingRepositoryUpload upload)
    throws BLException
  {
    final List<Path> files = upload.files();
    for (int fileIndex = 0, fileMax = files.size(); fileIndex < fileMax; ++fileIndex) {
      final Path file = files.get(fileIndex);

      final Path actual =
        upload.baseDirectory().resolve(file).toAbsolutePath();

      final String baseURI = this.configuration.baseURI().toString();
      final StringBuilder uriBuilder = new StringBuilder(128);
      uriBuilder.append(scrubTrailingSlashes(baseURI));
      uriBuilder.append("/service/local/staging/deployByRepositoryId/");
      uriBuilder.append(upload.repositoryId());
      uriBuilder.append("/");
      uriBuilder.append(translateFileToURIPath(file));
      final URI targetURI = URI.create(uriBuilder.toString());

      final StringBuilder serviceUriBuilder = new StringBuilder(128);
      serviceUriBuilder.append(scrubTrailingSlashes(baseURI));
      serviceUriBuilder.append("/service/local/staging/repository/");
      serviceUriBuilder.append(upload.repositoryId());
      final URI serviceURI = URI.create(serviceUriBuilder.toString());

      final BLRetryingUploader uploader =
        new BLRetryingUploader(
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

  private static String translateFileToURIPath(
    final Path file)
  {
    final FileSystem filesystem = file.getFileSystem();

    final Path relative;
    if (file.isAbsolute()) {
      final Path root = file.getRoot();
      relative = root.relativize(file);
    } else {
      relative = file;
    }

    return relative.toString()
      .replace(filesystem.getSeparator(), "/");
  }
}
