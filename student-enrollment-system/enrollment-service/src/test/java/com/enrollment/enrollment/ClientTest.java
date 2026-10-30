package com.enrollment.enrollment;

import com.enrollment.enrollment.client.CourseClient;
import org.junit.jupiter.api.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.assertj.core.api.Assertions.*;

class ClientTest {
    RestClient.Builder builder;
    MockRestServiceServer server;
    CourseClient client;
    @BeforeEach void setup() {
        builder=RestClient.builder(); server=MockRestServiceServer.bindTo(builder).build();
        client=new CourseClient(builder,"http://course-service");
    }
    @Test void successfulResponse() {
        server.expect(requestTo("http://course-service/api/courses/1")).andRespond(withSuccess("{\"id\":1,\"title\":\"Java\",\"credits\":4}",APPLICATION_JSON));
        assertThat(client.getCourseById(1L).getTitle()).isEqualTo("Java");
    }
    @Test void missingResource() {
        server.expect(anything()).andRespond(withStatus(NOT_FOUND));
        assertThatThrownBy(() -> client.getCourseById(1L)).hasMessageContaining("404");
    }
    @Test void unauthorizedDependencyIsNotMissingResource() {
        server.expect(anything()).andRespond(withStatus(UNAUTHORIZED));
        assertThatThrownBy(() -> client.getCourseById(1L)).hasMessageContaining("503");
    }
    @Test void unavailableDependency() {
        server.expect(anything()).andRespond(withStatus(INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> client.getCourseById(1L)).hasMessageContaining("503");
    }
    @Test void invalidBody() {
        server.expect(anything()).andRespond(withSuccess("{}",APPLICATION_JSON));
        assertThatThrownBy(() -> client.getCourseById(1L)).hasMessageContaining("503");
    }
    @Test void timeout() {
        server.expect(anything()).andRespond(withException(new java.net.SocketTimeoutException("Read timeout")));
        assertThatThrownBy(() -> client.getCourseById(1L)).hasMessageContaining("503");
    }
}
