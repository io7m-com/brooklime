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

import com.io7m.brooklime.api.BLNexusError;
import com.io7m.brooklime.api.BLParseException;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A provider of Nexus parsers.
 */

public final class BLNexusParsers
{
  /**
   * A provider of Nexus parsers.
   */

  public BLNexusParsers()
  {

  }

  private static List<Element> optionalChildElements(
    final Element element,
    final String childElement)
  {
    final NodeList childElements =
      element.getElementsByTagName(childElement);

    final ArrayList<Element> elements =
      new ArrayList<>(childElements.getLength());
    for (int index = 0, size = childElements.getLength(); index < size; ++index) {
      elements.add((Element) childElements.item(index));
    }
    return elements;
  }

  private static Element requireChildElement(
    final URI source,
    final Element element,
    final String childElement)
    throws BLParseException
  {
    final NodeList childElements =
      element.getElementsByTagName(childElement);

    if (childElements.getLength() == 0) {
      final BLPositionalXML.BLPosition position =
        BLPositionalXML.lexicalOf(element);
      throw new BLParseException(
        String.format(
          "Expected an element '%s' as a child of '%s'",
          childElement,
          element.getTagName()
        ),
        position.line(),
        position.column(),
        source
      );
    }

    return (Element) childElements.item(0);
  }

  private static void checkIsElement(
    final URI source,
    final Element element,
    final String expectedName)
    throws BLParseException
  {
    if (!Objects.equals(element.getTagName(), expectedName)) {
      final BLPositionalXML.BLPosition position =
        BLPositionalXML.lexicalOf(element);
      throw new BLParseException(
        String.format(
          "Expected an element '%s' but received '%s'",
          expectedName,
          element.getLocalName()
        ),
        position.line(),
        position.column(),
        source
      );
    }
  }

  /**
   * Parse a staging repository ID from the given stream.
   *
   * @param uri    The source URI
   * @param stream The stream URI
   *
   * @return A staging repository ID
   *
   * @throws BLParseException On errors
   */

  public String parseStagingRepositoryCreate(
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final Document document = BLPositionalXML.readXML(uri, stream);
      final Element root = document.getDocumentElement();
      checkIsElement(uri, root, "promoteResponse");

      final Element data =
        requireChildElement(uri, root, "data");
      final Element stagedRepositoryId =
        requireChildElement(uri, data, "stagedRepositoryId");

      return stagedRepositoryId.getTextContent().trim();
    } catch (final SAXParseException e) {
      throw new BLParseException(
        e.getMessage(),
        e,
        e.getLineNumber(),
        e.getColumnNumber(),
        uri
      );
    } catch (final Exception e) {
      throw new BLParseException(e.getMessage(), e, -1, -1, uri);
    }
  }

  /**
   * Parse a list of staging repositories from the given stream.
   *
   * @param uri    The source URI
   * @param stream The stream URI
   *
   * @return A list of staging repositories
   *
   * @throws BLParseException On errors
   */

  public List<BLStagingProfileRepository> parseRepositories(
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final Document document = BLPositionalXML.readXML(uri, stream);
      final Element root = document.getDocumentElement();
      checkIsElement(uri, root, "stagingRepositories");

      final Element data =
        requireChildElement(uri, root, "data");
      final List<Element> repositoryElements =
        optionalChildElements(data, "stagingProfileRepository");

      final List<BLStagingProfileRepository> repositories =
        new ArrayList<>(repositoryElements.size());
      for (final Element repositoryElement : repositoryElements) {
        repositories.add(this.parseRepository(uri, repositoryElement));
      }

      return Collections.unmodifiableList(repositories);
    } catch (final SAXParseException e) {
      throw new BLParseException(
        e.getMessage(),
        e,
        e.getLineNumber(),
        e.getColumnNumber(),
        uri
      );
    } catch (final Exception e) {
      throw new BLParseException(e.getMessage(), e, -1, -1, uri);
    }
  }

  /**
   * Parse a staging repository from the given stream.
   *
   * @param uri    The source URI
   * @param stream The stream URI
   *
   * @return A staging repository
   *
   * @throws BLParseException On errors
   */

  public BLStagingProfileRepository parseRepository(
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final Document document = BLPositionalXML.readXML(uri, stream);
      final Element root = document.getDocumentElement();
      return this.parseRepository(uri, root);
    } catch (final SAXParseException e) {
      throw new BLParseException(
        e.getMessage(),
        e,
        e.getLineNumber(),
        e.getColumnNumber(),
        uri
      );
    } catch (final Exception e) {
      throw new BLParseException(e.getMessage(), e, -1, -1, uri);
    }
  }

  private BLStagingProfileRepository parseRepository(
    final URI uri,
    final Element repositoryElement)
    throws BLParseException
  {
    final BLStagingProfileRepository.Builder builder =
      BLStagingProfileRepository.builder();

    builder.setCreated(
      OffsetDateTime.from(
        DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(
          requireChildElement(uri, repositoryElement, "created")
            .getTextContent().trim()
        )
      )
    );
    builder.setDescription(
      requireChildElement(uri, repositoryElement, "description")
        .getTextContent().trim()
    );
    builder.setIpAddress(
      requireChildElement(uri, repositoryElement, "ipAddress")
        .getTextContent().trim()
    );
    builder.setNotifications(
      requireChildElement(uri, repositoryElement, "notifications")
        .getTextContent().trim()
    );
    builder.setPolicy(
      requireChildElement(uri, repositoryElement, "policy")
        .getTextContent().trim()
    );
    builder.setProfileId(
      requireChildElement(uri, repositoryElement, "profileId")
        .getTextContent().trim()
    );
    builder.setProfileName(
      requireChildElement(uri, repositoryElement, "profileName")
        .getTextContent().trim()
    );
    builder.setProfileType(
      requireChildElement(uri, repositoryElement, "profileType")
        .getTextContent().trim()
    );
    builder.setProvider(
      requireChildElement(uri, repositoryElement, "provider")
        .getTextContent().trim()
    );
    builder.setRepositoryId(
      requireChildElement(uri, repositoryElement, "repositoryId")
        .getTextContent().trim()
    );
    builder.setReleaseRepositoryId(
      requireChildElement(uri, repositoryElement, "releaseRepositoryId")
        .getTextContent().trim()
    );
    builder.setReleaseRepositoryName(
      requireChildElement(uri, repositoryElement, "releaseRepositoryName")
        .getTextContent().trim()
    );
    builder.setTransitioning(
      Boolean.valueOf(
        requireChildElement(uri, repositoryElement, "transitioning")
          .getTextContent().trim()
      ).booleanValue()
    );
    builder.setType(
      requireChildElement(uri, repositoryElement, "type")
        .getTextContent().trim()
    );
    builder.setUserId(
      requireChildElement(uri, repositoryElement, "userId")
        .getTextContent().trim()
    );
    builder.setUserAgent(
      requireChildElement(uri, repositoryElement, "userAgent")
        .getTextContent().trim()
    );
    builder.setRepositoryURI(
      URI.create(
        requireChildElement(uri, repositoryElement, "repositoryURI")
          .getTextContent().trim()
      )
    );
    builder.setUpdated(
      OffsetDateTime.from(
        DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(
          requireChildElement(uri, repositoryElement, "updated")
            .getTextContent().trim()
        )
      )
    );

    return builder.build();
  }

  /**
   * Parse errors from the given stream.
   *
   * @param uri    The source URI
   * @param stream The stream URI
   *
   * @return Errors
   *
   * @throws BLParseException On errors
   */

  private List<BLNexusError> parseErrors(
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final Document document =
        BLPositionalXML.readXML(uri, stream);
      final Element root =
        document.getDocumentElement();
      return parseErrorsElement(uri, root);
    } catch (final SAXParseException e) {
      throw new BLParseException(
        e.getMessage(),
        e,
        e.getLineNumber(),
        e.getColumnNumber(),
        uri
      );
    } catch (final Exception e) {
      throw new BLParseException(e.getMessage(), e, -1, -1, uri);
    }
  }

  /**
   * Parse errors from the given stream, or return an empty list if the
   * content type does not indicate XML.
   *
   * @param contentType The content type
   * @param uri         The source URI
   * @param stream      The stream URI
   *
   * @return Errors
   *
   * @throws BLParseException On errors
   */

  public List<BLNexusError> parseErrorsIfPresent(
    final String contentType,
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(contentType, "contentType");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    if (contentType.startsWith("application/xml")) {
      return this.parseErrors(uri, stream);
    }
    return List.of();
  }

  private static List<BLNexusError> parseErrorsElement(
    final URI uri,
    final Element element)
    throws BLParseException
  {
    final var errorsElement =
      requireChildElement(uri, element, "errors");

    final List<Element> errorElements =
      optionalChildElements(errorsElement, "error");

    final var results = new ArrayList<BLNexusError>();
    for (final var errorElement : errorElements) {
      results.add(parseError(uri, errorElement));
    }
    return List.copyOf(results);
  }

  private static BLNexusError parseError(
    final URI uri,
    final Element element)
    throws BLParseException
  {
    final var idElement =
      requireChildElement(uri, element, "id");
    final var msgElement =
      requireChildElement(uri, element, "msg");

    return BLNexusError.builder()
      .setId(idElement.getTextContent().trim())
      .setMessage(msgElement.getTextContent().trim())
      .build();
  }
}
