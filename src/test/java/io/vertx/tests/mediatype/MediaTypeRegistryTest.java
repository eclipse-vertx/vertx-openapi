package io.vertx.tests.mediatype;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.mediatype.MediaTypeException;
import io.vertx.openapi.mediatype.MediaTypeRegistry;
import io.vertx.openapi.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.mediatype.MediaTypeRegistration.alwaysValid;
import static io.vertx.openapi.mediatype.MediaTypeRegistration.whitelist;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MediaTypeRegistryTest {

  @Test
  void emptyShouldNotSupportAnyMediaType() {
    var r = MediaTypeRegistry.createEmpty();
    assertThat(r.isSupported(MediaType.TEXT_PLAIN)).isFalse();
    assertThat(r.isSupported(MediaType.TEXT_PLAIN_UTF8)).isFalse();
    assertThat(r.isSupported(MediaType.APPLICATION_JSON)).isFalse();
    assertThat(r.isSupported(MediaType.APPLICATION_HAL_JSON)).isFalse();
    assertThat(r.isSupported(MediaType.APPLICATION_OCTET_STREAM)).isFalse();
    assertThat(r.isSupported(MediaType.MULTIPART_FORM_DATA)).isFalse();
  }

  @Test
  void defaultShouldSupportMediaTypes() {
    var r = MediaTypeRegistry.createDefault();
    assertThat(r.isSupported(MediaType.TEXT_PLAIN)).isTrue();
    assertThat(r.isSupported(MediaType.TEXT_PLAIN_UTF8)).isTrue();
    assertThat(r.isSupported(MediaType.APPLICATION_JSON)).isTrue();
    assertThat(r.isSupported(MediaType.APPLICATION_HAL_JSON)).isTrue();
    assertThat(r.isSupported(MediaType.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(r.isSupported(MediaType.MULTIPART_FORM_DATA)).isTrue();
  }

  @Test
  void addCustomTypeShouldMakeItSupported() {
    var r = MediaTypeRegistry.createEmpty();
    var t = "application/vnd.openxmlformats-officedocument.drawingml.diagramData+xml";
    r.register(alwaysValid(whitelist(t)));
    assertThat(r.isSupported(t)).isTrue();
  }


  @Test
  void createContentAnalyserShouldThrowExceptionWhenMediaTypeIsNotSupported() {
    var r = MediaTypeRegistry.createEmpty();
    assertThrows(
      MediaTypeException.class,
      () -> r.createContentAnalyser(
        "text/plain",
        Buffer.buffer(),
        ValidationContext.REQUEST)
    );
  }

  @Test
  void createContentAnalyserShouldWorkForKnownMimetype() {
    var r = MediaTypeRegistry.createDefault();
    var c = r.createContentAnalyser(
      "text/plain",
      Buffer.buffer(),
      ValidationContext.REQUEST);
    assertThat(c).isNotNull();
  }

}
