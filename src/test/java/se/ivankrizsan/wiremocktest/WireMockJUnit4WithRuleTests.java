package se.ivankrizsan.wiremocktest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Examples on how to use WireMock with JUnit4.
 * This class uses a JUnit @Rule to create and destroy the WireMock server.
 *
 * @author Ivan Krizsan
 */
public class WireMockJUnit4WithRuleTests extends AbstractTestBase {
    /* Constant(s): */

    /* Instance variable(s): */
    @Rule
    public WireMockRule mWireMockRule = new WireMockRule(HTTP_ENDPOINT_PORT);

    /**
     * Performs preparations before each test.
     */
    @Before
    public void setup() {
        initializeRestAssuredHttp();
    }

    /**
     * Test sending a HTTP request to the mock server that does not match the request
     * that the mock server expects.
     * This example shows how WireMock behaves when using a JUnit rule to create the
     * WireMock server.
     *
     * Expected result: A response with the HTTP status not-found should be received and
     * a {@code VerificationException} should be thrown when the JUnit rule is evaluated,
     * after completion of the test.
     */
    @Test
    public void mismatchingRequestTest() {
        /*
         * Test HTTP mock expects one request to /wiremock/test with an Accept header
         * that has the value "application/json".
         * When having received such a request, it will return a response with
         * the HTTP status 200 and the header Content-Type with the value "text/plain".
         * The body of the response will be a string containing a greeting.
         */
        stubFor(
            get(urlEqualTo(BASE_PATH))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .withBody("Hello World, we have a Houston!")
                )
        );

        /*
         * Send the test-request and save the response so we can log information from it.
         * No verification of the response is undertaken, as a content-type that the mock
         * server is not expecting is deliberately sent and the .
         * This is done in order to examine the response in cases like this.
         */
        final Response theResponse = RestAssured
            .given()
            .contentType(ContentType.XML)
            .accept(ContentType.TEXT)
            .when()
            .get(BASE_HTTP_URL);

        /*
         * In a normal test the HTTP status of the response would be expected to be OK (200),
         * but since the content-type of the request is deliberately chosen not to match,
         * a NOT-FOUND (404) will be returned.
         */
        Assert.assertEquals("The HTTP status should be not-found, since "
                + "a request was sent that was not expected by WireMock",
            HttpStatus.NOT_FOUND.value(), theResponse.getStatusCode());

        /* Log information from the response for the sake of this being an example. */
        logResponseStatusHeadersAndBody(theResponse);
    }
}
