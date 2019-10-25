package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import javax.inject.Inject;

import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author: adavid9
 */
@IntegrationTest
public class ActuatorEndpointControllerTest
        extends RestAssuredBaseTest
{

    private static final String LOGGER_PACKAGE = "org.carlspring.strongbox";

    private static final String METRIC_NAME = "process.start.time";

    private static final String NOT_EXISTING_METRIC_NAME = "process.start.time.test";

    @Inject
    private PropertiesBooter propertiesBooter;

    @Inject
    private MockMvcRequestSpecification mockMvc;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
        setContextBaseUrl(getContextBaseUrl() + "/api/monitoring");
    }

    @Test
    @WithUserDetails("admin")
    public void testStrongboxInfo()
    {

        String url = getContextBaseUrl() + "/info";

        mockMvc.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("strongbox", notNullValue());

        String version = propertiesBooter.getStrongboxVersion();

        mockMvc.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("strongbox.version", equalTo(version));

        String revision = propertiesBooter.getStrongboxRevision();

        mockMvc.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body("strongbox.revision", equalTo(revision));
    }

    @Test
    public void testMonitoringEndpointWithAuthorizedUser()
    {
        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testMonitoringEndpointWithUnauthorizedUser()
    {
        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(getContextBaseUrl())
               .peek()
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testHealthEndpointWithAuthorizedUser()
    {
        String url = getContextBaseUrl() + "/health";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testHealthEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/health";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testHealthComponentEndpointWithAuthorizedUser()
    {
        String url = getContextBaseUrl() + "/health/db";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testHealthComponentEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/health/db";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testHealthNotExistingComponentEndpoint()
    {
        String url = getContextBaseUrl() + "/health/not_existing_component";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void testInfoEndpointWithAuthorizedUser()
    {
        String url = getContextBaseUrl() + "/info";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testInfoEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/info";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testBeansEndpointWithAuthorizedUser()
    {
        String url = getContextBaseUrl() + "/beans";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testBeansEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/beans";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testMetricsEndpointWithAuthorizedUser()
    {

        String url = getContextBaseUrl() + "/metrics";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testMetricsEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/metrics";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testSingleMetricEndpointWithAuthorizedUser()
    {
        String url = getContextBaseUrl() + "/metrics/" + METRIC_NAME;

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testSingleMetricEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/metrics/" + METRIC_NAME;

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testNotExistingMetricEndpoint()
    {
        String url = getContextBaseUrl() + "/metrics/" + NOT_EXISTING_METRIC_NAME;

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .peek()
               .then()
               .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void testLoggersEndpointWithAuthorizedUser()
    {
        String url = getContextBaseUrl() + "/loggers";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testLoggersEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/loggers";

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }

    @Test
    public void testLoggersSinglePackageEndpointWithAuthorizedUser()
    {
        String url = getContextBaseUrl() + "/loggers/" + LOGGER_PACKAGE;

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.OK.value())
               .body(CoreMatchers.notNullValue());
    }

    @Test
    @WithAnonymousUser
    public void testLoggersSinglePackageEndpointWithUnauthorizedUser()
    {
        String url = getContextBaseUrl() + "/loggers/" + LOGGER_PACKAGE;

        mockMvc.accept(MediaType.APPLICATION_JSON_VALUE)
               .when()
               .get(url)
               .then()
               .statusCode(HttpStatus.UNAUTHORIZED.value())
               .body("error", CoreMatchers.equalTo(getI18nInsufficientAuthenticationErrorMessage()));
    }
}
