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

import com.io7m.brooklime.api.BLErrorLogging;
import com.io7m.brooklime.api.BLException;
import com.io7m.brooklime.api.BLHTTPErrorException;
import com.io7m.brooklime.api.BLHTTPFailureException;
import com.io7m.brooklime.vanilla.internal.streamtime.STTimedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * An uploader that retries on failure.
 */

public final class BLRetryingUploader
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLRetryingUploader.class);

  private final HttpClient client;
  private final URI serviceURI;
  private final URI targetURI;
  private final Path file;
  private final int fileIndex;
  private final int fileCount;
  private final Duration retryDelay;
  private final int maxRetries;
  private final BLProgressCounter counter;
  private final ScheduledExecutorService executor;
  private final BLNexusParsers parsers;

  /**
   * An uploader that retries on failure.
   *
   * @param inExecutor   A scheduled executor for statistics
   * @param inClient     The HTTP client
   * @param inServiceURI The service URI
   * @param inTargetURI  The target URI
   * @param inFile       The file
   * @param inFileIndex  The file index
   * @param inFileCount  The file count
   * @param inRetryDelay The retry delay
   * @param inMaxRetries The maximum number of retries
   * @param inCounter    The progress counter
   */

  public BLRetryingUploader(
    final ScheduledExecutorService inExecutor,
    final HttpClient inClient,
    final URI inServiceURI,
    final URI inTargetURI,
    final Path inFile,
    final int inFileIndex,
    final int inFileCount,
    final Duration inRetryDelay,
    final int inMaxRetries,
    final BLProgressCounter inCounter)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "inExecutor");
    this.client =
      Objects.requireNonNull(inClient, "inClient");
    this.serviceURI =
      Objects.requireNonNull(inServiceURI, "inServiceURI");
    this.targetURI =
      Objects.requireNonNull(inTargetURI, "targetURI");
    this.file =
      Objects.requireNonNull(inFile, "inFile");
    this.fileIndex =
      inFileIndex;
    this.fileCount =
      inFileCount;
    this.retryDelay =
      Objects.requireNonNull(inRetryDelay, "inRetryDelay");
    this.maxRetries =
      inMaxRetries;
    this.counter =
      Objects.requireNonNull(inCounter, "inCounter");
    this.parsers =
      new BLNexusParsers();

    if (!this.file.isAbsolute()) {
      throw new IllegalArgumentException("File must be absolute");
    }
  }

  /**
   * Execute the upload.
   *
   * @throws BLException On errors
   */

  public void execute()
    throws BLException
  {
    for (int attempt = 0; attempt < this.maxRetries; ++attempt) {
      try {
        final long sizeExpected = Files.size(this.file);

        this.counter.startFile(
          this.file.toString(),
          sizeExpected,
          attempt + 1,
          this.maxRetries,
          this.fileIndex,
          this.fileCount
        );

        final Supplier<InputStream> inputStreamSupplier = () -> {
          try {
            final var baseStream =
              Files.newInputStream(this.file);

            final var timedStream =
              new STTimedInputStream(
                this.executor,
                OptionalLong.of(sizeExpected),
                statistics -> {
                  this.counter.setSizeReceived(statistics.sizeTransferred());
                },
                baseStream
              );

            return timedStream;
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
        };

        final var put =
          HttpRequest.newBuilder(this.targetURI)
            .PUT(BodyPublishers.ofInputStream(inputStreamSupplier))
            .header("Content-Type", "application/octet-stream")
            .build();

        final var response =
          this.client.send(put, HttpResponse.BodyHandlers.ofInputStream());

        final int status = response.statusCode();
        if (status >= 400) {
          LOG.error(
            "{}: {}",
            this.targetURI,
            Integer.valueOf(status)
          );

          final var errors =
            this.parsers.parseErrorsIfPresent(
              contentTypeOf(response),
              this.targetURI,
              response.body()
            );

          BLErrorLogging.logErrors(LOG, errors);
          throw new BLHTTPErrorException(status, errorOf(status), errors);
        }
        return;
      } catch (final Exception e) {
        LOG.debug("I/O error: ", e);
      }

      try {
        LOG.debug("sleeping for {} before retrying", this.retryDelay);
        Thread.sleep(this.retryDelay.toMillis());
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    throw new BLHTTPFailureException(
      String.format(
        "Failed to upload file %s after %d attempts",
        this.file,
        Integer.valueOf(this.maxRetries))
    );
  }

  private static String contentTypeOf(
    final HttpResponse<InputStream> response)
  {
    return response.headers()
      .firstValue("Content-Type")
      .orElse("application/octet-stream");
  }

  private static String errorOf(
    final int status)
  {
    return "Error: %d".formatted(Integer.valueOf(status));
  }
}
