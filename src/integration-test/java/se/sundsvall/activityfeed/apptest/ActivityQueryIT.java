package se.sundsvall.activityfeed.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.activityfeed.apptest.Constants.CORRELATION_ID;
import static se.sundsvall.activityfeed.apptest.Constants.CORRELATION_ID_HEADER;

/**
 * The query interface through the whole service against MariaDB.
 *
 * @see src/test/resources/db/scripts/testdata-it.sql for test data.
 */
@WireMockAppTestSuite(files = "classpath:/ActivityQueryIT/", classes = Application.class)
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ActivityQueryIT extends AbstractAppTest {

	private static final String PATH = "/2281/kundhandelseFragaSynkron";

	@Test
	void test01_queryByCustomer() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withHeader("Accept-Language", "sv")
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_queryWithFilterSortingAndPagination() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_invalidQuery() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withRequest("request.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_municipalityNotConfigured() {
		setupCall()
			.withServicePath("/1480/kundhandelseFragaSynkron")
			.withHttpMethod(POST)
			.withHeader(CORRELATION_ID_HEADER, CORRELATION_ID)
			.withRequest("request.json")
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}
}
