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

import java.net.URI;
import java.util.Objects;

/**
 * The type of exceptions caused by failing to parse data.
 */

public class BLParseException extends BLException
{
  private final int line;
  private final int column;
  private final URI source;

  /**
   * Construct an exception.
   *
   * @param message  The message
   * @param inLine   The line number
   * @param inColumn The column number
   * @param inSource The source URI
   */

  public BLParseException(
    final String message,
    final int inLine,
    final int inColumn,
    final URI inSource)
  {
    super(message);
    this.line = inLine;
    this.column = inColumn;
    this.source = Objects.requireNonNull(inSource, "source");
  }

  /**
   * Construct an exception.
   *
   * @param message  The message
   * @param cause    The cause
   * @param inLine   The line number
   * @param inColumn The column number
   * @param inSource The source URI
   */

  public BLParseException(
    final String message,
    final Throwable cause,
    final int inLine,
    final int inColumn,
    final URI inSource)
  {
    super(message, cause);
    this.line = inLine;
    this.column = inColumn;
    this.source = Objects.requireNonNull(inSource, "source");
  }
}
