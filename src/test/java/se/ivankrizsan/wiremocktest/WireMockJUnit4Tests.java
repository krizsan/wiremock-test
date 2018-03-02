package se.ivankrizsan.wiremocktest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static io.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * Examples on how to use WireMock with JUnit4.
 * This class creates and destroys the WireMock server programmatically. It also
 * programmatically retrieves unmatched requests from the WireMock server and
 * logs these as errors.
 * An alternative is to use the WireMock server as a JUnit @Rule, please refer to
 * {@link WireMockJUnit4WithRuleTests} for an example.
 *
 * @author Ivan Krizsan
 */
public class WireMockJUnit4Tests extends AbstractTestBase {
    /* Constant(s): */
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockJUnit4Tests.class);
    protected static final String TESTFILES_BASE =
        "se/ivankrizsan/wiremocktest/";
    public static final String EXCHANGE_RATE = "4.123";

    /* Instance variable(s): */
    protected WireMockServer mWireMockServer;

    /**
     * Performs preparations before each test.
     */
    @Before
    public void setup() {
        initializeRestAssuredHttp();

        /*
         * Create the WireMock server to be used by a test.
         * This also ensures that the records of received requests kept by the WireMock
         * server and expected scenarios etc are cleared prior to each test.
         * An alternative is to create the WireMock server once before all the tests in
         * a test-class and call {@code resetAll} before each test.
         */
        mWireMockServer = new WireMockServer(HTTP_ENDPOINT_PORT);
        mWireMockServer.start();
    }

    /**
     * Performs cleanup after each test.
     */
    @After
    public void tearDown() {
        /* Stop the WireMock server. */
        mWireMockServer.stop();

        /*
         * Find all requests that were expected by the WireMock server but that were
         * not matched by any request actually made to the server.
         * Logs any such requests as errors.
         */
        final List<LoggedRequest> theUnmatchedRequests = mWireMockServer.findAllUnmatchedRequests();
        if (!theUnmatchedRequests.isEmpty()) {
            LOGGER.error("Unmatched requests: {}", theUnmatchedRequests);
        }
    }

    /**
     * Test sending a HTTP request to the mock server that matches the request that
     * the mock server expects.
     *
     * Expected result: A response with an expected HTTP status (418) and a response body
     * containing a greeting should be returned.
     */
    @Test
    public void matchingRequestTest() {
        /*
         * Setup test HTTP mock as to expect one request to /wiremock/test with an Accept
         * header that has the value "text/plain".
         * When having received such a request, the mock will return a response with
         * the HTTP status 418 and the header Content-Type with the value "text/plain".
         * The body of the response will be a string containing a greeting.
         */
        mWireMockServer.stubFor(
            get(urlEqualTo(BASE_PATH))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN_VALUE))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatus.I_AM_A_TEAPOT.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .withBody("Hello client, this is the response body.")
                )
        );

        /* Send the test-request and save the response so we can log information from it. */
        final Response theResponse = RestAssured
            .given()
            .contentType(ContentType.TEXT)
            .accept(ContentType.TEXT)
            .when()
            .get(BASE_HTTP_URL);
        theResponse
            .then()
            .statusCode(HttpStatus.I_AM_A_TEAPOT.value())
            .contentType(ContentType.TEXT);

        /* Log information from the response for the sake of this being an example. */
        logResponseStatusHeadersAndBody(theResponse);

        /* Verify the result. */
        Assert.assertThat("Response message should contain greeting",
            theResponse.asString(), new StringContains("Hello"));
    }

    /**
     * Test sending a HTTP request to the mock server that does not match the request
     * that the mock server expects.
     * This example shows how WireMock behaves when the WireMock server is created
     * programmatically as opposed to using a JUnit 4 Rule to create the server.
     *
     * Expected result: A response with HTTP status 404 should be returned and a payload
     * explaining the difference(s) between the actual and expected request.
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
        mWireMockServer.stubFor(
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

        /* Log information from the response for the sake of the example. */
        logResponseStatusHeadersAndBody(theResponse);
    }

    /**
     * Tests sending a request to the mock server to which the mock server will respond
     * with a 10 second delay.
     *
     * Expected result: A response containing a greeting should be received after
     * a delay exceeding a certain period of time.
     */
    @Test
    public void slowResponseTest() {
        /*
         * Setup test HTTP mock as to expect one request to /wiremock/test with an Accept
         * header that has the value "text/plain".
         * When having received such a request, the mock will return a response with
         * the HTTP status 200 and the header Content-Type with the value "text/plain".
         * The body of the response will be a string containing a greeting.
         * There will be a delay of 10 seconds for each request to the mock service.
         */
        mWireMockServer.stubFor(
            get(urlEqualTo(BASE_PATH))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.TEXT_PLAIN_VALUE))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .withBody("Hello client, this is the response body.")
                        .withFixedDelay(DEFAULT_TIMEOUT * 2)
                )
        );

        /* Send the test-request. */
        RestAssured
            .given()
            .contentType(ContentType.TEXT)
            .accept(ContentType.TEXT)
            .when()
            .get(BASE_HTTP_URL)
            .then()
            .time(greaterThan((long) DEFAULT_TIMEOUT))
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.TEXT);
    }

    /**
     * Tests sending a request to the mock server where the mock server will respond
     * with the contents of a template-file in which values are inserted.
     *
     * Expected result: A response containing an XML fragment should be received
     * with an element containing an exchange rate.
     */
    @Test
    public void responseTemplateTest() {
        /* Stop the default WireMock server, since a custom one is needed for this test. */
        mWireMockServer.stop();

        /*
         * Create the response transformer that will insert values into the response
         * templates. Note that the transformer is not global, that is it is not automatically
         * applied to all responses from the WireMock server.
         */
        final ResponseTemplateTransformer theTemplateTransformer =
            new ResponseTemplateTransformer(false);
        final String theTemplateTransformerName = theTemplateTransformer.getName();
        mWireMockServer = new WireMockServer(
            WireMockConfiguration
                .options()
                .port(HTTP_ENDPOINT_PORT)
                .extensions(theTemplateTransformer));
        mWireMockServer.start();
        /*
         * Setup test HTTP mock as to expect one request to /wiremock/test with an Accept
         * header that has the value "application/xml".
         * When having received such a request, the mock will return a response with
         * the HTTP status 200 and the header Content-Type with the value "text/plain".
         * The body of the response will be a the contents of a file located at
         * "src/test/resources/__files/se/ivankrizsan/wiremocktest/soap-response.xml"
         * in the project.
         * The value of the request HTTP header "exchangerate" will be inserted into
         * the response created from the template-file.
         */
        mWireMockServer.stubFor(
            get(urlEqualTo(BASE_PATH))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_XML_VALUE))
                .willReturn(
                    aResponse()
                        .withBodyFile(TESTFILES_BASE + "soap-response.xml")
                        .withTransformers(theTemplateTransformerName)
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                )
        );

        /*
         * Send test-request to the mock server.
         * Note that XML namespace awareness is switched off in order to avoid having
         * to specify namespace mappings.
         * XPath expressions will work against the local names of elements and ignore
         * any namespaces used in the XML.
         */
        final Response theResponse = RestAssured
            .given()
            .config(RestAssured.config().xmlConfig(xmlConfig().with().namespaceAware(false)))
            .contentType(ContentType.XML)
            .accept(MediaType.APPLICATION_XML_VALUE)
            .header("exchangerate", EXCHANGE_RATE)
            .when()
            .get(BASE_HTTP_URL);

        /*
         * Verify that the response is OK, is of the XML type and contains the exchange rate
         * in the expected element in the XML.
         */
        theResponse
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.XML)
            .body(
                hasXPath(
                    "/Envelope/Body/ConversionRateResponse/ConversionRateResult",
                    containsString(EXCHANGE_RATE))
            );

        logResponseStatusHeadersAndBody(theResponse);
    }
}
