package se.ivankrizsan.wiremocktest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;

/**
 * Examples on how to use HTTPS with client authentication with WireMock
 * and REST Assured.
 * To debug the SSL handshake, launch with this property: -Djavax.net.debug=ssl:handshake
 *
 * @author Ivan Krizsan
 */
public class WireMockHttpsWithClientAuthTests extends AbstractTestBase {
    /* Constant(s): */
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockHttpsWithClientAuthTests.class);

    /* Instance variable(s): */
    protected WireMockServer mWireMockServer;

    /**
     * Initializes REST Assured for HTTPS communication. To be called before each test.
     */
    @Before
    public void initializeRestAssuredHttps() {
        initializeRestAssuredHttp();

        /*
         * Create the WireMock server to be used by a test.
         * This also ensures that the records of received requests kept by the WireMock
         * server and expected scenarios etc are cleared prior to each test.
         * An alternative is to create the WireMock server once before all the tests in
         * a test-class and call {@code resetAll} before each test.
         */
        final WireMockConfiguration theWireMockConfiguration = wireMockConfig()
            .httpsPort(HTTPS_ENDPOINT_PORT)
            .needClientAuth(true)
            .keystorePath(SERVER_KEYSTORE_PATH)
            .keystorePassword(SERVER_KEYSTORE_PASSWORD)
            .trustStorePath(SERVER_TRUSTSTORE_PATH)
            .trustStorePassword(SERVER_TRUSTSTORE_PASSWORD);
        mWireMockServer = new WireMockServer(theWireMockConfiguration);
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
     * Test sending a HTTPS request using REST Assured to the mock server that matches the request that
     * the mock server expects.
     * Expected result: A response containing a greeting should be received from the server.
     *
     * NOTE!
     * REST Assured seems to be unable to do HTTPS with mutual authentication if the client
     * keystore and truststore is configured using the {@code keyStore} and {@code trustStore}
     * methods. The problem can be worked around by creating a custom SSL socket factory configured
     * with the client keystore and truststore.
     *
     * Expected result: A response containing a greeting should be received.
     *
     * @throws Exception If error occurs setting up the client SSL socket factory for REST Assured.
     */
    @Test
    public void successfulWithClientAuthRestAssuredTest() throws Exception {
        /*
         * Setup test HTTPS mock as to expect one request to /wiremock/test with an Accept
         * header that has the value "text/plain".
         * When having received such a request, the mock will return a response with
         * the HTTP status 200 and the header Content-Type with the value "text/plain".
         * The body of the response will be a string containing a greeting.
         */
        mWireMockServer.stubFor(
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
         * Setting up REST Assured using the keyStore and trustStore methods does not work
         * so a custom SSL socket factory need to be created.
         * If some day REST Assured fixes the problem, the client keystore and truststore
         * could be configured using the following after the {@code given()} method call:
         * {@code
         *     .relaxedHTTPSValidation()
         *     .keyStore(CLIENT_KEYSTORE_PATH, CLIENT_KEYSTORE_PASSWORD)
         *     .trustStore(CLIENT_TRUSTSTORE_PATH, CLIENT_TRUSTSTORE_PASSWORD)
         * }
         * In such a case, the call to the {@code config} method should be removed.
         *
         * In addition, REST Assured only allows for using the deprecated {@code SSLSocketFactory}
         * instead of the recommended {@code SSLConnectionSocketFactory}.
         * This class contains a method to create the latter, in the case REST Assured is
         * some day updated - please see {@code createNewClientSSLSocketFactory}.
         */
        @SuppressWarnings("deprecation")
        final SSLSocketFactory theClientSSLSocketFactory = createOldClientSSLSocketFactory();
        /* Send the test-request and save the response so we can log information from it. */
        final Response theResponse = RestAssured
            .given()
            .config(
                newConfig()
                .sslConfig(new SSLConfig().sslSocketFactory(theClientSSLSocketFactory))
            )
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

    /**
     * Creates the client SSL connection socket factory configured to use the client keystore
     * and truststores.
     * This method creates the newer type of factory that is not deprecated.
     * Regretfully, the current version of REST Assured does not accept this type of factory,
     * but only the old deprecated type. See {@code createOldClientSSLSocketFactory} for the
     * method that create the type of factory that REST Assured is able to use.
     *
     * @return Client SSL connection socket factory.
     * @throws Exception If error occurs creating the factory.
     */
    private SSLConnectionSocketFactory createNewClientSSLSocketFactory() throws Exception {
        final KeyManager[] theClientKeyManagers = createClientKeyManagers();

        final TrustManager[] theClientTrustManagers = createClientTrustManagers();

        /*
         * The client SSL context contains both client key and trust managers
         * since mutual server requires HTTPS mutual authentication.
         */
        SSLContext theClientSSLContext = SSLContext.getInstance("TLS");
        theClientSSLContext.init(theClientKeyManagers, theClientTrustManagers, null);

        /*
         * Create a client SSL connection socket factory using the client's SSL context and
         * a hostname verifier that disables hostname verification.
         * The NOOP hostname verifier is used since we are using self-signed certificates
         * with a CN that does not match the hostname.
         */
        return new SSLConnectionSocketFactory(theClientSSLContext, new NoopHostnameVerifier());
    }

    /**
     * Creates the client SSL connection socket factory configured to use the client keystore
     * and truststores.
     * This method creates the old type of factory that is deprecated.
     *
     * @return Client SSL connection socket factory.
     * @throws Exception If error occurs creating the factory.
     */
    private SSLSocketFactory createOldClientSSLSocketFactory() throws Exception {
        final KeyManager[] theClientKeyManagers = createClientKeyManagers();

        final TrustManager[] theClientTrustManagers = createClientTrustManagers();

        /*
         * The client SSL context contains both client key and trust managers
         * since mutual server requires HTTPS mutual authentication.
         */
        SSLContext theClientSSLContext = SSLContext.getInstance("TLS");
        theClientSSLContext.init(theClientKeyManagers, theClientTrustManagers, null);

        /*
         * Create a client SSL connection socket factory using the client's SSL context and
         * a hostname verifier that disables hostname verification.
         * The NOOP hostname verifier is used since we are using self-signed certificates
         * with a CN that does not match the hostname.
         */
        final SSLSocketFactory theClientSSLSocketFactory = new SSLSocketFactory(theClientSSLContext);
        theClientSSLSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        return theClientSSLSocketFactory;
    }

    /**
     * Creates the client trust managers using the client truststore.
     *
     * @return Array of client trust managers.
     * @throws Exception If error occurs creating key managers.
     */
    private TrustManager[] createClientTrustManagers() throws Exception {
        final KeyStore theClientTruststore = getClientTruststore();

        /* Create the trust manager factory which is responsible for creating the client trust managers. */
        final TrustManagerFactory theClientTrustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        theClientTrustManagerFactory.init(theClientTruststore);

        return theClientTrustManagerFactory.getTrustManagers();
    }

    /**
     * Creates the client key managers using the client keystore.
     * Loads the client keystore from the file system.
     *
     * @return Array of client key managers.
     * @throws Exception If error occurs creating key managers.
     */
    private KeyManager[] createClientKeyManagers() throws Exception {
        final KeyStore theClientKeystore = getClientKeystore();

        /* Create the key manager factory which is responsible for creating the client key managers. */
        final KeyManagerFactory theClientKeyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        theClientKeyManagerFactory.init(theClientKeystore, CLIENT_KEYSTORE_PASSWORD.toCharArray());

        return theClientKeyManagerFactory.getKeyManagers();
    }

    /**
     * Creates and loads the client truststore.
     * Truststore is loaded from the file system.
     *
     * @return Client truststore.
     * @throws Exception If error occurs loading truststore.
     */
    private KeyStore getClientTruststore() throws Exception {
        final KeyStore theClientTruststore = KeyStore.getInstance("JKS");
        theClientTruststore.load(
            new FileInputStream(CLIENT_TRUSTSTORE_PATH), CLIENT_TRUSTSTORE_PASSWORD.toCharArray());

        return theClientTruststore;
    }

    /**
     * Creates and loads the client keystore.
     * Keystore is loaded from the file system.
     *
     * @return Keystore object containing client keystore.
     * @throws Exception If error occurs loading keystore.
     */
    private KeyStore getClientKeystore() throws Exception {
        final KeyStore theClientKeystore = KeyStore.getInstance("JKS");
        theClientKeystore.load(
            new FileInputStream(CLIENT_KEYSTORE_PATH), CLIENT_KEYSTORE_PASSWORD.toCharArray());

        return theClientKeystore;
    }
}
