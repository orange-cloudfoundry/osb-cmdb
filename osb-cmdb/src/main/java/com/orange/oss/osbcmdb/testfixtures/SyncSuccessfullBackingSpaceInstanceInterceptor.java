package com.orange.oss.osbcmdb.testfixtures;

import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * Simulates a successful synchronous backing service requested in backing space.
 *
 * Note: doesn't add behavior w.r.t. base class, only here to have explicit naming when used.
 */
public class SyncSuccessfullBackingSpaceInstanceInterceptor extends BaseServiceInstanceBackingSpaceInstanceInterceptor {

	private static final Logger LOG = Loggers.getLogger(SyncSuccessfullBackingSpaceInstanceInterceptor.class);

	public SyncSuccessfullBackingSpaceInstanceInterceptor(String defaultSpaceName) {
		super(defaultSpaceName);
	}

}
