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

package com.io7m.brooklime.api;

import org.immutables.value.Value;

import java.time.Duration;

import static com.io7m.brooklime.api.BLProgressEventType.Kind.PROGRESS_FILE_STARTED;
import static com.io7m.brooklime.api.BLProgressEventType.Kind.PROGRESS_UPDATE;

public interface BLProgressEventType
{
  Kind kind();

  String name();

  int fileIndexCurrent();

  int fileIndexMaximum();

  int attemptCurrent();

  int attemptMaximum();

  enum Kind
  {
    PROGRESS_FILE_STARTED,
    PROGRESS_UPDATE
  }

  @Value.Immutable
  @BLImmutableStyleType
  interface BLProgressFileStartedType extends BLProgressEventType
  {
    @Override
    default Kind kind()
    {
      return PROGRESS_FILE_STARTED;
    }

    @Override
    String name();

    @Override
    int attemptCurrent();

    @Override
    int attemptMaximum();

    @Override
    int fileIndexCurrent();

    @Override
    int fileIndexMaximum();
  }

  @Value.Immutable
  @BLImmutableStyleType
  interface BLProgressUpdateType extends BLProgressEventType
  {
    @Override
    default Kind kind()
    {
      return PROGRESS_UPDATE;
    }

    @Override
    String name();

    @Override
    int fileIndexCurrent();

    @Override
    int fileIndexMaximum();

    @Override
    int attemptCurrent();

    @Override
    int attemptMaximum();

    long bytesSent();

    long bytesMaximum();

    double progress();

    long bytesPerSecond();

    Duration timeRemaining();
  }
}
