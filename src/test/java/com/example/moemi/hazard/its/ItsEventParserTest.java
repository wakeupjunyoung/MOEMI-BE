package com.example.moemi.hazard.its;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.moemi.hazard.GeoPoint;
import com.example.moemi.hazard.HazardFetchException;

class ItsEventParserTest {

    @Test
    void parsesEveryField() throws IOException {
        List<ItsEvent> events = ItsEventParser.parse(fixture("its/event-info.xml"));

        assertThat(events).hasSize(3);
        ItsEvent construction = events.get(0);
        assertThat(construction.roadType()).isEqualTo("국도");
        assertThat(construction.eventType()).isEqualTo("공사");
        assertThat(construction.eventDetailType()).isEqualTo("도로포장");
        assertThat(construction.startAt()).isEqualTo(LocalDateTime.of(2020, 10, 13, 0, 0));
        assertThat(construction.endAt()).isEqualTo(LocalDateTime.of(2020, 12, 10, 0, 0));
        assertThat(construction.location()).isEqualTo(new GeoPoint(35.072289, 127.148574));
        assertThat(construction.roadName()).isEqualTo("국도22호선");
        assertThat(construction.direction()).isEqualTo("화순방향");
        assertThat(construction.blockType()).isEqualTo("전면통제");
        assertThat(construction.blockedLanes()).isNull();
        assertThat(construction.message()).isEqualTo("동복터널 종점부 화순방향 전면통제 낙석산사태 정비공사");
    }

    @Test
    void blankFieldsBecomeNull() throws IOException {
        ItsEvent work = ItsEventParser.parse(fixture("its/event-info.xml")).get(2);

        assertThat(work.endAt()).isNull();
        assertThat(work.blockType()).isNull();
        assertThat(work.blockedLanes()).isEqualTo("2차로 차단");
    }

    @Test
    void failureResultCodeThrows() {
        byte[] body = """
                <response><header><resultCode>1</resultCode><resultMsg>FAIL</resultMsg></header></response>
                """.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ItsEventParser.parse(body))
                .isInstanceOf(HazardFetchException.class)
                .hasMessageContaining("resultCode=1");
    }

    @Test
    void nonXmlThrows() {
        assertThatThrownBy(() -> ItsEventParser.parse("Unauthorized".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(HazardFetchException.class);
    }

    @Test
    void rejectsDoctypeToPreventXxe() {
        byte[] body = """
                <?xml version="1.0"?>
                <!DOCTYPE response [<!ENTITY xxe SYSTEM "file:///etc/hostname">]>
                <response><header><resultCode>0</resultCode><resultMsg>&xxe;</resultMsg></header></response>
                """.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ItsEventParser.parse(body)).isInstanceOf(HazardFetchException.class);
    }

    private static byte[] fixture(String path) throws IOException {
        try (InputStream in = ItsEventParserTest.class.getClassLoader().getResourceAsStream(path)) {
            return in.readAllBytes();
        }
    }
}
