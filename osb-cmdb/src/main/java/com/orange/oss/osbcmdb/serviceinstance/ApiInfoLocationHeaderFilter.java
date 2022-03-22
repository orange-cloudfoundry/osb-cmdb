package com.orange.oss.osbcmdb.serviceinstance;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiInfoLocationHeaderFilter extends HttpFilter  {

	protected final Logger LOG = Loggers.getLogger(ApiInfoLocationHeaderFilter.class);

	private String expectedXApiInfoLocationHeader;

	private boolean rejectRequestsWithNonMatchingXApiInfoLocationHeader;

	private String localCloudfoundryApiHostToWhiteList;

	/**
	 * Json serializer used to format message. Saved as field instance for optimization
	 * "Mapper instances are fully thread-safe provided that ALL configuration of the instance occurs before ANY read
	 * or write calls." says javadoc.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();


	public ApiInfoLocationHeaderFilter(String expectedXApiInfoLocationHeader,
		boolean rejectRequestsWithNonMatchingXApiInfoLocationHeader, String localCloudfoundryApiHostToWhiteList) {
		this.expectedXApiInfoLocationHeader = expectedXApiInfoLocationHeader;
		this.rejectRequestsWithNonMatchingXApiInfoLocationHeader = rejectRequestsWithNonMatchingXApiInfoLocationHeader;
		this.localCloudfoundryApiHostToWhiteList = localCloudfoundryApiHostToWhiteList;
		LOG.info("Configuring ApiInfoLocationHeaderFilter with rejectRequestsWithNonMatchingXApiInfoLocationHeader={}" +
			" and expectedXApiInfoLocationHeader={}", rejectRequestsWithNonMatchingXApiInfoLocationHeader,
			expectedXApiInfoLocationHeader);
	}


	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws IOException, ServletException {

		if (shouldEnforceXApiInfoLocation(request.getRequestURI())) {
			String xApiInfoLocation = request.getHeader("X-Api-Info-Location");
			LOG.trace("inspecting http header X-Api-Info-Location={}", xApiInfoLocation);

			String expectedXApiInfoLocation = this.expectedXApiInfoLocationHeader;
			if (!shouldAcceptXApiInfoLocation(xApiInfoLocation, expectedXApiInfoLocation)) {
				LOG.info("rejecting request from X-Api-Info-Location={} whereas expecting {}", xApiInfoLocation,
					expectedXApiInfoLocation);

				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setHeader("Content-Type", "application/json");
				PrintWriter writer = response.getWriter();
				writer.write(formatRejectedXApiInfoLocation(xApiInfoLocation, expectedXApiInfoLocation));
				writer.flush();
				response.flushBuffer();
				return;
			}
		}
		chain.doFilter(request, response);
	}

	protected String formatRejectedXApiInfoLocation(String xApiInfoLocation, String expectedXApiInfoLocation) {
		//Avoid json injection from headers: let Jackson escape the quotes
		Map<String, String> map = new HashMap<>();
		map.put("description", " Request not received from white listed osb-cmdb client." +
			" Please, double check configuration mistakes." +
			" Expecting X-Api-Info-Location http header value:" + expectedXApiInfoLocation + " but got: " + xApiInfoLocation);
		try {
			return objectMapper.writeValueAsString(map);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean shouldAcceptXApiInfoLocation(String receivedXApiInfoLocation, String expectedXApiInfoLocation) {
		if (! localCloudfoundryApiHostToWhiteList.isEmpty()) {
			String whitelistedLocalApiInfo = localCloudfoundryApiHostToWhiteList + "/v2/info";
			if (whitelistedLocalApiInfo.equalsIgnoreCase(receivedXApiInfoLocation)) {
				return true;
			}
		}
		return expectedXApiInfoLocation.equalsIgnoreCase(receivedXApiInfoLocation);
	}

	protected boolean shouldEnforceXApiInfoLocation(String requestURI) {
		boolean shouldEnforceXApiInfoLocation= true;
		if (rejectRequestsWithNonMatchingXApiInfoLocationHeader == false ||
			expectedXApiInfoLocationHeader == null ||
			expectedXApiInfoLocationHeader.isEmpty()) {
			shouldEnforceXApiInfoLocation=false;
		}
		if (! requestURI.startsWith(("/v2"))) {
			shouldEnforceXApiInfoLocation=false;
		}
		return shouldEnforceXApiInfoLocation;
	}

	@Override
	public void init(FilterConfig filterConfig) {
	}

	@Override
	public void destroy() {
	}
}