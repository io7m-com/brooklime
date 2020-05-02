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

package com.io7m.brooklime.cmdline.internal;

import ch.qos.logback.classic.Level;
import com.io7m.junreachable.UnreachableCodeException;

import java.util.Objects;

/**
 * Log level.
 */

public enum BLLogLevel
{
  /**
   * @see Level#TRACE
   */

  LOG_TRACE("trace"),

  /**
   * @see Level#DEBUG
   */

  LOG_DEBUG("debug"),

  /**
   * @see Level#INFO
   */

  LOG_INFO("info"),

  /**
   * @see Level#WARN
   */

  LOG_WARN("warn"),

  /**
   * @see Level#ERROR
   */

  LOG_ERROR("error");


  private final String name;

  BLLogLevel(final String in_name)
  {
    this.name = Objects.requireNonNull(in_name, "Name");
  }

  @Override
  public String toString()
  {
    return this.name;
  }

  /**
   * @return The short name of the level
   */

  public String getName()
  {
    return this.name;
  }

  Level toLevel()
  {
    switch (this) {
      case LOG_TRACE:
        return Level.TRACE;
      case LOG_DEBUG:
        return Level.DEBUG;
      case LOG_INFO:
        return Level.INFO;
      case LOG_WARN:
        return Level.WARN;
      case LOG_ERROR:
        return Level.ERROR;
    }

    throw new UnreachableCodeException();
  }
}
