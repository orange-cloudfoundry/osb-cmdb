package com.orange.oss.osbcmdb.serviceinstance;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiInfoLocationHeaderFilter extends HttpFilter  {

	protected final Logger LOG = Loggers.getLogger(ApiInfoLocationHeaderFilter.class);

	private String expectedXApiInfoLocationHeader;

	private boolean rejectRequestsWithNonMatchingXApiInfoLocationHeader;

	public ApiInfoLocationHeaderFilter(String expectedXApiInfoLocationHeader,
		boolean rejectRequestsWithNonMatchingXApiInfoLocationHeader) {
		this.expectedXApiInfoLocationHeader = expectedXApiInfoLocationHeader;
		this.rejectRequestsWithNonMatchingXApiInfoLocationHeader = rejectRequestsWithNonMatchingXApiInfoLocationHeader;
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
			if (! expectedXApiInfoLocation.equalsIgnoreCase(xApiInfoLocation)) {
				LOG.error("rejecting request from X-Api-Info-Location={} whereas expecting {}", xApiInfoLocation, expectedXApiInfoLocation);

				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setHeader("Content-Type", "application/json");
				PrintWriter writer = response.getWriter();
				writer.write("{\"description\":\"" +
					" Request not received from white listed osb-cmdb client." +
					" Please, double check configuration mistakes." +
					" Expecting X-Api-Info-Location http header value:" + expectedXApiInfoLocation+ "\"}");
				//Note: we don't output the current header to avoid json injection from clients.
				writer.flush();
				response.flushBuffer();
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private boolean shouldEnforceXApiInfoLocation(String requestURI) {
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