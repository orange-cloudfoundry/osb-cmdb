package com.orange.oss.osbcmdb;

import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuactorConfig {

	/**
	 * Required to activate http tracing, see https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-http-tracing
	 * @return
	 */
	@Bean
	public HttpTraceRepository httpTraceRepository() {
		return new InMemoryHttpTraceRepository();
	}
}
