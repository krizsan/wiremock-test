package se.ivankrizsan.wiremocktest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Examples on how to use HTTPS without client authentication with WireMock
 * and REST Assured.
 * To debug the SSL handshake, launch with this property: -Djavax.net.debug=ssl:handshake
 *
 * @author Ivan Krizsan
 */
public class WireMockHttpsNoClientAuthTests extends AbstractTestBase {
    /* Constant(s): */

    /* Instance variable(s): */
    @Rule
    public WireMockRule mWireMockRule = new WireMockRule(wireMockConfig()
        .httpsPort(HTTPS_ENDPOINT_PORT)
        .keystorePath(SERVER_KEYSTORE_PATH)
        .keystorePassword(SERVER_KEYSTORE_PASSWORD));

    /**
     * Initializes REST Assured for HTTPS communication. To be called before each test.
     */
    @Before
    public void setup() {
        initializeRestAssuredHttp();
    }

    /**
     * Test sending a HTTPS request to the mock server that matches the request that
     * the mock server expects. Client authentication is not used.
     *
     * Expected result: A response containing a greeting should be received.
     */
    @Test
    public void successfulNoClientAuthTest() {
        /*
         * Setup test HTTPS mock as to expect one request to /wiremock/test with an Accept
         * header that has the value "text/plain".
         * When having received such a request, the mock will return a response with
         * the HTTP status 200 and the header Content-Type with the value "text/plain".
         * The body of the response will be a string containing a greeting.
         */
        stubFor(
            get(urlEqualTo(BASE_PATH))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN_VALUE))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .withBody("Hello client, this is the response body.")
                )
        );

        /*
         * Send the test-request and save the response so we can log information from it.
         * Set the client keystore to be used with this request.
         * Since a self-signed certificate is used, HTTPS validation need to be relaxed.
         * This is needed if your certificate is not signed by a CA or if the
         * name in the certificate does not match the DNS name of the host.
         */
        final Response theResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .keyStore(CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASSWORD)
            .contentType(ContentType.TEXT)
            .accept(ContentType.TEXT)
            .when()
            .get(BASE_HTTPS_URL);
        theResponse
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.TEXT);

        /* Log information from the response for the sake of this being an example. */
        logResponseStatusHeadersAndBody(theResponse);

        /* Verify the result. */
        Assert.assertThat("Response message should contain greeting",
            theResponse.asString(), new StringContains("Hello"));
    }
}
