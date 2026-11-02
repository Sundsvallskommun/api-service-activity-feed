package se.sundsvall.activityfeed.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.activityfeed.Application;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.activityfeed.apptest.Constants.ADMIN_KEY;
import static se.sundsvall.activityfeed.apptest.Constants.API_KEY_HEADER;
import static se.sundsvall.activityfeed.apptest.Constants.QUERY_KEY;

/**
 * Maintenance of the activity cache through the whole service against MariaDB.
 *
 * @see src/test/resources/db/scripts/testdata-it.sql for test data.
 */
@WireMockAppTestSuite(files = "classpath:/ActivityIT/", classes = Application.class)
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ActivityIT extends AbstractAppTest {

	private static final String PATH = "/2281/kundhandelser";

	@Test
	void test01_saveActivities() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withRequest("request.json")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();

		// The replaced activity has new content, the new one exists, and the timestamp is returned with a Swedish offset.
		setupCall()
			.withServicePath(PATH + "/TESTKOP-BYGG-2026-00123-1")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("replaced.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("count.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getActivity() {
		setupCall()
			.withServicePath(PATH + "/TESTKOP-BYGG-2026-00123-2")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_deleteActivity() {
		setupCall()
			.withServicePath(PATH + "/TESTKOP-FSK-2026-00777-1")
			.withHttpMethod(DELETE)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH + "/TESTKOP-FSK-2026-00777-1")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_activityNotVisibleFromOtherMunicipality() {
		setupCall()
			.withServicePath("/2262/kundhandelser/TESTKOP-BYGG-2026-00123-1")
			.withHttpMethod(GET)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_queryKeyCannotSave() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, QUERY_KEY)
			.withRequest("request.json")
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_wrongPrefix() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(API_KEY_HEADER, ADMIN_KEY)
			.withRequest("request.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}
}
