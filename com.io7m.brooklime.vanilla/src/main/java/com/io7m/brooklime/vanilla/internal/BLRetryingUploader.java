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
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import static org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.hc.core5.http.HttpStatus.SC_CLIENT_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

public final class BLRetryingUploader
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLRetryingUploader.class);

  private final CloseableHttpClient client;
  private final URI serviceURI;
  private final URI targetURI;
  private final Path file;
  private final int fileIndex;
  private final int fileCount;
  private final Duration retryDelay;
  private final int maxRetries;
  private final BLProgressCounter counter;

  public BLRetryingUploader(
    final CloseableHttpClient inClient,
    final URI inServiceURI,
    final URI inTargetURI,
    final Path inFile,
    final int inFileIndex,
    final int inFileCount,
    final Duration inRetryDelay,
    final int inMaxRetries,
    final BLProgressCounter inCounter)
  {
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

    if (!this.file.isAbsolute()) {
      throw new IllegalArgumentException("File must be absolute");
    }
  }

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

        /*
         * First, make a HEAD request to the URL to make sure that the
         * credentials are correct. If the client succeeds in passing whatever
         * authentication challenge the server sends, the credentials are
         * cached in the given HTTP context for later reuse in the PUT call.
         */

        final HttpClientContext context = new HttpClientContext();
        final HttpHead head = new HttpHead(this.serviceURI);
        try (CloseableHttpResponse response = this.client.execute(
          head,
          context)) {
          final int status = response.getCode();
          if (status >= SC_CLIENT_ERROR && status <= SC_INTERNAL_SERVER_ERROR) {
            LOG.error(
              "{}: {} {}",
              this.serviceURI,
              Integer.valueOf(status),
              response.getReasonPhrase()
            );
            throw new BLHTTPErrorException(status, response.getReasonPhrase());
          }
        }

        final HttpPut put = new HttpPut(this.targetURI);
        put.setEntity(new BLStreamEntity(
          this.counter,
          this.file,
          APPLICATION_OCTET_STREAM));

        try (CloseableHttpResponse response = this.client.execute(
          put,
          context)) {
          final int status = response.getCode();
          if (status >= SC_CLIENT_ERROR && status <= SC_INTERNAL_SERVER_ERROR) {
            LOG.error(
              "{}: {} {}",
              this.targetURI,
              Integer.valueOf(status),
              response.getReasonPhrase()
            );
            throw new BLHTTPErrorException(status, response.getReasonPhrase());
          }
          return;
        }
      } catch (final IOException | BLHTTPErrorException e) {
        LOG.debug("i/o error: ", e);
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
}
