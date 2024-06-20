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

/**
 * The type of progress events.
 */

public interface BLProgressEventType
{
  /**
   * @return The event kind
   */

  Kind kind();

  /**
   * @return The event name
   */

  String name();

  /**
   * @return The current file index
   */

  int fileIndexCurrent();

  /**
   * @return The current maximum file index
   */

  int fileIndexMaximum();

  /**
   * @return The current attempt number
   */

  int attemptCurrent();

  /**
   * @return The maximum number of attempts
   */

  int attemptMaximum();

  /**
   * The kind of events.
   */

  enum Kind
  {
    /**
     * An operation started for a given file.
     *
     * @see BLProgressFileStartedType
     */

    PROGRESS_FILE_STARTED,

    /**
     * Progress was made.
     *
     * @see BLProgressUpdateType
     */

    PROGRESS_UPDATE
  }

  /**
   * Progress started on a particular file.
   */

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

  /**
   * Progress was made on a particular file.
   */

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

    /**
     * @return The number of bytes sent so far
     */

    long bytesSent();

    /**
     * @return The number of bytes that will be sent
     */

    long bytesMaximum();

    /**
     * @return The progress as a real value
     */

    double progress();

    /**
     * @return The number of bytes per second
     */

    long bytesPerSecond();

    /**
     * @return An estimate of the remaining time
     */

    Duration timeRemaining();
  }
}
