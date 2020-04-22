package com.orange.oss.osbcmdb.testfixtures;

import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * Simulates a successful synchronous backing service requested in backing space.
 *
 * Note: doesn't add behavior w.r.t. base class, only here to have explicit naming when used.
 */
public class SyncSuccessfulBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(SyncSuccessfulBackingSpaceInstanceInterceptor.class);

	public SyncSuccessfulBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

}
