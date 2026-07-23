package com.nattechnologies.trastcms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.ContentSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentSanitizerTests {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContentSanitizer sanitizer = new ContentSanitizer(objectMapper);

    @Test
    void sanitizesNestedHtmlInsideBuilderDocument() throws Exception {
        String value = """
                {"version":1,"blocks":[{"id":"one","type":"richText","data":{"html":"<p>Seguro</p><script>alert(1)</script>"},"style":{}}],"global":{"containerWidth":1200,"gap":24}}
                """;

        JsonNode result = objectMapper.readTree(sanitizer.json(value));

        assertThat(result.at("/blocks/0/data/html").asText()).isEqualTo("<p>Seguro</p>");
    }

    @Test
    void rejectsUnsafeUrlInsideBuilderDocument() {
        String value = """
                {"version":1,"blocks":[{"id":"one","type":"button","data":{"href":"javascript:alert(1)"},"style":{}}],"global":{"containerWidth":1200,"gap":24}}
                """;

        assertThatThrownBy(() -> sanitizer.json(value))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("URL no permitida");
    }


    @Test
    void preservesSafeRichTextStylesAndMediaAttributes() {
        String value = """
                <p style="text-align:center;position:fixed;color:#6d4aff">Texto</p>
                <mark data-color="#fef08a" style="background-color:#fef08a">Marca</mark>
                <video data-trast-media="true" kind="VIDEO" contenttype="video/mp4" src="/api/public/media/demo" controls></video>
                """;

        String result = sanitizer.html(value);

        assertThat(result).contains("text-align:center", "color:#6d4aff");
        assertThat(result).doesNotContain("position:fixed");
        assertThat(result).contains("data-trast-media=\"true\"", "kind=\"VIDEO\"");
        assertThat(result).contains("src=\"/api/public/media/demo\"");
    }


    @Test
    void preservesRelativeUrlsForSupportedRichMediaElements() {
        String value = """
                <img src="/api/public/media/image" alt="Imagen">
                <audio src="/api/public/media/audio" controls></audio>
                <video src="/api/public/media/video"
                       poster="/api/public/media/poster"
                       controls>
                    <source src="/api/public/media/video-source" type="video/mp4">
                </video>
                <a href="/page/contacto">Contacto</a>
                """;

        String result = sanitizer.html(value);

        assertThat(result)
                .contains("src=\"/api/public/media/image\"")
                .contains("src=\"/api/public/media/audio\"")
                .contains("src=\"/api/public/media/video\"")
                .contains("poster=\"/api/public/media/poster\"")
                .contains("src=\"/api/public/media/video-source\"")
                .contains("href=\"/page/contacto\"");
    }

    @Test
    void removesUnsafeProtocolsFromRichMediaUrls() {
        String value = """
                <img src="javascript:alert(1)" alt="Imagen">
                <audio src="data:text/html,alert(1)" controls></audio>
                <video src="javascript:alert(1)"
                       poster="javascript:alert(1)"
                       controls>
                    <source src="vbscript:alert(1)" type="video/mp4">
                </video>
                <a href="javascript:alert(1)">Enlace</a>
                """;

        String result = sanitizer.html(value);

        assertThat(result)
                .doesNotContain("javascript:")
                .doesNotContain("data:text/html")
                .doesNotContain("vbscript:");
    }

    @Test
    void removesUnsafeInlineCss() {
        String result = sanitizer.html("""
                <p style="background-image:url(javascript:alert(1));text-align:right">Texto</p>
                """);

        assertThat(result).doesNotContain("javascript", "background-image");
    }

    @Test
    void rejectsDocumentsWithoutBuilderContract() {
        assertThatThrownBy(() -> sanitizer.json("{\"blocks\":[]}"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("formato válido");
    }
}
