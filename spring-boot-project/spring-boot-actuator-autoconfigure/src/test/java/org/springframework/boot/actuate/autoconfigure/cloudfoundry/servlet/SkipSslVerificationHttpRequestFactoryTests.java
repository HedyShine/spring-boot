/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import javax.net.ssl.SSLHandshakeException;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.testsupport.web.servlet.ExampleServlet;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Test for {@link SkipSslVerificationHttpRequestFactory}.
 */
public class SkipSslVerificationHttpRequestFactoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private WebServer webServer;

	@After
	public void shutdownContainer() {
		if (this.webServer != null) {
			this.webServer.stop();
		}
	}

	@Test
	public void restCallToSelfSignedServerShouldNotThrowSslException() throws Exception {
		String httpsUrl = getHttpsUrl();
		SkipSslVerificationHttpRequestFactory requestFactory = new SkipSslVerificationHttpRequestFactory();
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		ResponseEntity<String> responseEntity = restTemplate.getForEntity(httpsUrl,
				String.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		this.thrown.expect(ResourceAccessException.class);
		this.thrown.expectCause(isSSLHandshakeException());
		RestTemplate otherRestTemplate = new RestTemplate();
		otherRestTemplate.getForEntity(httpsUrl, String.class);
	}

	private Matcher<Throwable> isSSLHandshakeException() {
		return instanceOf(SSLHandshakeException.class);
	}

	private String getHttpsUrl() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		factory.setSsl(getSsl("password", "classpath:test.jks"));
		this.webServer = factory.getWebServer(
				new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
		this.webServer.start();
		return "https://localhost:" + this.webServer.getPort() + "/hello";
	}

	private Ssl getSsl(String keyPassword, String keyStore) {
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setKeyPassword(keyPassword);
		ssl.setKeyStore(keyStore);
		ssl.setKeyStorePassword("secret");
		return ssl;
	}

}
