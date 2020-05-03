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

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class BLStreamEntity extends AbstractHttpEntity
{
  private final BLProgressCounter counter;
  private final Path file;

  public BLStreamEntity(
    final BLProgressCounter inCounter,
    final Path inFile,
    final ContentType contentType)
  {
    super(contentType, null);

    this.counter = Objects.requireNonNull(inCounter, "counter");
    this.file = Objects.requireNonNull(inFile, "file");
  }

  @Override
  public boolean isRepeatable()
  {
    return true;
  }

  @Override
  public long getContentLength()
  {
    try {
      return Files.size(this.file);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public InputStream getContent()
    throws IOException
  {
    return Files.newInputStream(this.file);
  }

  @Override
  public void writeTo(final OutputStream outStream)
    throws IOException
  {
    Objects.requireNonNull(outStream, "Output stream");

    try (InputStream inStream = Files.newInputStream(this.file)) {
      final byte[] buffer = new byte[4096];
      while (true) {
        final int bytesRead = inStream.read(buffer);
        if (bytesRead < 0) {
          break;
        }

        outStream.write(buffer, 0, bytesRead);
        this.counter.addSizeReceived(bytesRead);
      }
    }
  }

  @Override
  public boolean isStreaming()
  {
    return false;
  }

  @Override
  public void close()
    throws IOException
  {

  }
}
