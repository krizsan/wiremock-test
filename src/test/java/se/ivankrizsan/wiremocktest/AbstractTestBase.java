package se.ivankrizsan.wiremocktest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base-class for tests containing common constants and methods.
 *
 * @author Ivan Krizsan
 */
public abstract class AbstractTestBase {
    /* Constant(s): */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestBase.class);

    protected static final int HTTP_ENDPOINT_PORT = 8123;
    protected static final int HTTPS_ENDPOINT_PORT = 8443;
    protected static final String BASE_PATH = "/wiremocktest/hello";
    protected static final String BASE_HTTP_URL =
        "http://localhost:" + HTTP_ENDPOINT_PORT + BASE_PATH;
    protected static final String BASE_HTTPS_URL =
        "https://localhost:" + HTTPS_ENDPOINT_PORT + BASE_PATH;
    protected static final int DEFAULT_TIMEOUT = 5000;
    /* Client keystore and truststore. Self-signed. */
    protected static final String CLIENT_KEYSTORE_PATH = "client/client_keystore.jks";
    protected static final String CLIENT_KEYSTORE_PASSWORD = "secret";
    protected static final String CLIENT_TRUSTSTORE_PATH = "client/client_cacerts.jks";
    protected static final String CLIENT_TRUSTSTORE_PASSWORD = "secret";
    /* Server keystore and truststore. Self-signed. */
    protected static final String SERVER_KEYSTORE_PATH = "client/server/server_keystore.jks";
    protected static final String SERVER_KEYSTORE_PASSWORD = "secret";
    protected static final String SERVER_TRUSTSTORE_PATH = "client/server/server_cacerts.jks";
    protected static final String SERVER_TRUSTSTORE_PASSWORD = "secret";

    /**
     * Initializes REST Assured for plain HTTP communication. To be called before each test.
     */
    protected void initializeRestAssuredHttp() {
        RestAssured.reset();
        RestAssured.port = HTTP_ENDPOINT_PORT;
    }

    /**
     * Logs the HTTP status, the HTTP headers and the body from the supplied response.
     *
     * @param inResponseEntity Response for which to log information.
     */
    protected void logResponseStatusHeadersAndBody(final Response inResponseEntity) {
        LOGGER.info("Response status: {}", inResponseEntity.getStatusCode());
        LOGGER.info("Response headers: {}", inResponseEntity.getHeaders());
        LOGGER.info("Response body: " + inResponseEntity.getBody().asString());
    }
}
