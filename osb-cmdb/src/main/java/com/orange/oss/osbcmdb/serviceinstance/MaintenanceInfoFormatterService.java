package com.orange.oss.osbcmdb.serviceinstance;

import java.util.Objects;

import de.skuzzle.semantic.Version;
import org.apache.commons.lang3.StringUtils;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerMaintenanceInfoConflictException;
import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.lang.Nullable;

import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * Handles the logic related to service instance upgrades.
 */
public class MaintenanceInfoFormatterService {

	/**
	 * Configuration prefix for the MaintenanceInfo configuration bean
	 */
	public static final String PROPERTY_PREFIX = "osbcmdb.maintenanceinfo";

	/**
	 * The default maintenance info when a backend has no maintenance info declared. Supports logics of detecting
	 * backend MI changes.
	 */
	private static final MaintenanceInfo DEFAULT_MISSING_BACKEND_MI = MaintenanceInfo.builder()
		.version(0, 0, 0, "")
		.description("")
		.build();

	protected final Logger LOG = Loggers.getLogger(MaintenanceInfoFormatterService.class);

	/**
	 * When configured as non-null, proposes maintenance bumps to OSB users. This enables refresh of service
	 * instances view from OSB client when OSB-cmdb changes SI external view (typically the returned dashboard URL
	 * and its associated format)
	 * <p>
	 * When configured as null, then acts as a pass-thru to the maintenance info proposed by the backend
	 */
	@Nullable
	private MaintenanceInfo osbCmdbMaintenanceInfo;

	public MaintenanceInfoFormatterService(MaintenanceInfo osbCmdbMaintenanceInfo) {
		LOG.info("MaintenanceInfo configured is {}", osbCmdbMaintenanceInfo);
		this.osbCmdbMaintenanceInfo = osbCmdbMaintenanceInfo;
	}

	/**
	 * Formats MI passed to backend service instance upgrade requests.
	 */
	public org.cloudfoundry.client.v2.MaintenanceInfo formatForBackendInstance(UpdateServiceInstanceRequest request) {
		if (!hasMaintenanceInfoChangeRequest(request)) {
			return null;
		}
		if (osbCmdbMaintenanceInfo == null) {
			MaintenanceInfo requestMaintenanceInfo = request.getMaintenanceInfo();
			if (requestMaintenanceInfo == null) { //overkill as already tested above ?
				return null;
			}
			return org.cloudfoundry.client.v2.MaintenanceInfo.builder()
				.version(requestMaintenanceInfo.getVersion())
				.description(requestMaintenanceInfo.getDescription())
				.build();
		}
		MaintenanceInfo brokeredServiceMI = request.getPlan().getMaintenanceInfo();
		MaintenanceInfo inferredBackendMI = unmergeInfos(brokeredServiceMI);
		if (DEFAULT_MISSING_BACKEND_MI.equals(inferredBackendMI)) {
			return null;
		}
		return org.cloudfoundry.client.v2.MaintenanceInfo.builder()
			.version(inferredBackendMI.getVersion())
			.description(inferredBackendMI.getDescription())
			.build();
	}

	/**
	 * Formats for brokered service catalog returned to OSB client
	 */
	public MaintenanceInfo formatForCatalog(MaintenanceInfo backendCatalogMaintenanceInfo) {
		if (osbCmdbMaintenanceInfo == null) {
			return backendCatalogMaintenanceInfo;
		}
		if (backendCatalogMaintenanceInfo != null) {
			return mergeInfos(backendCatalogMaintenanceInfo);
		}
		return osbCmdbMaintenanceInfo;
	}

	/**
	 * Only used in tests
	 * @return
	 */
	public MaintenanceInfo getOsbCmdbMaintenanceInfo() {
		return this.osbCmdbMaintenanceInfo;
	}

	/**
	 * Indicates whether the request is a pure upgrade request `cf update-service --upgrade` resulting from a version
	 * bump introduced by osb-cmdb, and no backend version bump.
	 *
	 * @param request
	 * @return False if the update request should be passed to tbe backend broker, true if backend update should be
	 * 	skipped (i.e. noop)
	 */
	public boolean isNoOpUpgradeBackingService(UpdateServiceInstanceRequest request) {
		if (osbCmdbMaintenanceInfo == null) {
			return false; //act as a pass-through, i.e. rely on osb-client to prevent/optimize noop-upgrade requests
		}
		if (!hasMaintenanceInfoChangeRequest(request)) {
			return false;
		}
		MaintenanceInfo brokeredServiceMI = request.getPlan().getMaintenanceInfo();
		MaintenanceInfo inferredBackendMI = unmergeInfos(brokeredServiceMI);
		return inferredBackendMI.equals(DEFAULT_MISSING_BACKEND_MI);
	}

	/**
	 * Validates incoming USI request and rejects requests with unsupported maintenance info
	 */
	public void validateAnyUpgradeRequest(UpdateServiceInstanceRequest request) {
		MaintenanceInfo catalogMi = request.getPlan().getMaintenanceInfo();
		MaintenanceInfo requestedMi = request.getMaintenanceInfo();
		validateRequestedMaintenanceInfo(requestedMi, catalogMi);
	}

	protected MaintenanceInfo unmergeInfos(MaintenanceInfo brokeredMaintenanceInfo) {
		Version backendVersion = Version.parseVersion(brokeredMaintenanceInfo.getVersion());
		Version cmdbVersion = Version.parseVersion(osbCmdbMaintenanceInfo.getVersion());

		String extension = StringUtils.removeEnd(backendVersion.getBuildMetaData(),
			formatBuildMetadataSuffix(cmdbVersion));
		extension = StringUtils.removeEnd(extension, ".");
		extension = StringUtils.removeEnd(extension, cmdbVersion.getBuildMetaData());
		if (!extension.isEmpty()) {
			extension = "+" + extension;
		}
		String description = StringUtils.removeEnd(brokeredMaintenanceInfo.getDescription(),
			"\n" + osbCmdbMaintenanceInfo.getDescription());
		description = StringUtils.removeEnd(description, osbCmdbMaintenanceInfo.getDescription());
		int major = backendVersion.getMajor() - cmdbVersion.getMajor();
		int minor = backendVersion.getMinor() - cmdbVersion.getMinor();
		int patch = backendVersion.getPatch() - cmdbVersion.getPatch();
		return MaintenanceInfo.builder()
			.version(major,
				minor,
				patch,
				extension)
			.description(description)
			.build();
	}

	boolean hasMaintenanceInfoChangeRequest(UpdateServiceInstanceRequest request) {
		boolean hasMaintenanceInfoChangeRequest = false;
		if (request.getPreviousValues() == null) {
			LOG.warn("Received an USI without previous value, assuming not an upgrade request (likely not a CF " +
				"client)");
			return false;
		}
		if (request.getMaintenanceInfo() != null) {
			hasMaintenanceInfoChangeRequest =
				!request.getMaintenanceInfo().equals(request.getPreviousValues().getMaintenanceInfo());
		}
		return hasMaintenanceInfoChangeRequest;
	}

	MaintenanceInfo mergeInfos(MaintenanceInfo backendMaintenanceInfo) {
		Version backendVersion = Version.parseVersion(backendMaintenanceInfo.getVersion());
		Version cmdbVersion = Version.parseVersion(osbCmdbMaintenanceInfo.getVersion());
		String backendBuildMetaData = backendVersion.getBuildMetaData();
		String buildMetaData = "+";
		if (!backendBuildMetaData.isEmpty()) {
			buildMetaData = buildMetaData + backendBuildMetaData + ".";
		}
		buildMetaData = buildMetaData + formatBuildMetadataSuffix(cmdbVersion);
		String description = defaultString(backendMaintenanceInfo.getDescription());
		if (osbCmdbMaintenanceInfo.getDescription() != null) {
			description = description + "\n" + osbCmdbMaintenanceInfo.getDescription();
		}
		return MaintenanceInfo.builder()
			.version(backendVersion.getMajor() + cmdbVersion.getMajor(),
				backendVersion.getMinor() + cmdbVersion.getMinor(),
				backendVersion.getPatch() + cmdbVersion.getPatch(),
				buildMetaData)
			.description(description)
			.build();
	}

	void validateAnyCreateRequest(CreateServiceInstanceRequest request) {
		MaintenanceInfo catalogMi = request.getPlan().getMaintenanceInfo();
		MaintenanceInfo requestedMi = request.getMaintenanceInfo();
		validateRequestedMaintenanceInfo(requestedMi, catalogMi);
	}

	private String formatBuildMetadataSuffix(Version cmdbVersion) {
		return "osb-cmdb."
			+ cmdbVersion.getMajor() + "."
			+ cmdbVersion.getMinor() + "."
			+ cmdbVersion.getPatch()
			+ (cmdbVersion.getBuildMetaData().isEmpty() ? "" : "." + cmdbVersion.getBuildMetaData());
	}

	private void validateRequestedMaintenanceInfo(MaintenanceInfo requestedMi, MaintenanceInfo catalogMi) {
		if (requestedMi != null && !Objects.equals(requestedMi.getVersion(), catalogMi.getVersion())) {
			throw new ServiceBrokerMaintenanceInfoConflictException("unknown requested maintenance info: " + requestedMi
				.getVersion() + " Currently supported maintenance info is: " + catalogMi.getVersion());
		}
	}

}
