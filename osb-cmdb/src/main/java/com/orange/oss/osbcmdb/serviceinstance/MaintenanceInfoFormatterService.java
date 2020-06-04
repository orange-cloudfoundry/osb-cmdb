package com.orange.oss.osbcmdb.serviceinstance;

import de.skuzzle.semantic.Version;
import org.apache.commons.lang3.StringUtils;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerMaintenanceInfoConflictException;
import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class MaintenanceInfoFormatterService {
	protected final Logger LOG = Loggers.getLogger(MaintenanceInfoFormatterService.class);

	private static final MaintenanceInfo DEFAULT_MISSING_BACKEND_MI = MaintenanceInfo.builder()
		.version(0, 0, 0, "")
		.description("")
		.build();

	private MaintenanceInfo osbCmdbMaintenanceInfo;

	public MaintenanceInfoFormatterService(MaintenanceInfo osbCmdbMaintenanceInfo) {
		this.osbCmdbMaintenanceInfo = osbCmdbMaintenanceInfo;
	}

	/**
	 * Formats for brokered service catalog
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

	public MaintenanceInfo formatForBackendInstance(MaintenanceInfo backendCatalogMaintenanceInfo,
		MaintenanceInfo existingInstanceCatalogMaintenanceInfo) {
		if (osbCmdbMaintenanceInfo == null) {
			if (backendCatalogMaintenanceInfo == null) {
				if (existingInstanceCatalogMaintenanceInfo == null) {
					LOG.warn("Service instance has maintenance info while catalog has none, suspecting catalog " +
						"rollback");
				}
			}
			return existingInstanceCatalogMaintenanceInfo;
		} else {
			if (backendCatalogMaintenanceInfo != null) {
				return mergeInfos(backendCatalogMaintenanceInfo);
			}
			return osbCmdbMaintenanceInfo;
		}
	}

	/**
	 * Indicates whether the request is a pure upgrade request `cf update-service --upgrade` resulting from a version
	 * bump introduced by osb-cmdb, and no backend version bump.
	 * @param request
	 * @return
	 */
	public boolean isNoOpUpgradeBackingService(UpdateServiceInstanceRequest request) {
		boolean hasMaintenanceInfoChangeRequest = false;
		if (request.getPreviousValues() == null) {
			LOG.warn("Received an USI without previous value, assuming not an upgrade request (likely not a CF " +
				"client)");
			return false;
		}
		if (request.getMaintenanceInfo() != null) {
			hasMaintenanceInfoChangeRequest =
				! request.getMaintenanceInfo().equals(request.getPreviousValues().getMaintenanceInfo());
		}
		if (! hasMaintenanceInfoChangeRequest) {
			return false;
		}
		MaintenanceInfo brokeredServiceMI = request.getPlan().getMaintenanceInfo();
		MaintenanceInfo inferredBackendMI = unmergeInfos(brokeredServiceMI);
		return inferredBackendMI.equals(DEFAULT_MISSING_BACKEND_MI);
	}

	public MaintenanceInfo mergeInfos(MaintenanceInfo backendMaintenanceInfo) {
		Version backendVersion = Version.parseVersion(backendMaintenanceInfo.getVersion());
		Version cmdbVersion = Version.parseVersion(osbCmdbMaintenanceInfo.getVersion());
		String backendBuildMetaData = backendVersion.getBuildMetaData();
		String buildMetaData = "+";
		if (! backendBuildMetaData.isEmpty()) {
			buildMetaData = buildMetaData+ backendBuildMetaData + ".";
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


	public void validateAnyUpgradeRequest(UpdateServiceInstanceRequest request) {
		MaintenanceInfo catalogMi = request.getPlan().getMaintenanceInfo();
		MaintenanceInfo requestedMi = request.getMaintenanceInfo();
		if (requestedMi != null && ! requestedMi.equals(catalogMi)) {
			throw new ServiceBrokerMaintenanceInfoConflictException("unknown requested maintenance info: " + requestedMi + " Currently supported maintenance info is: " + catalogMi);
		}
	}

	public void validateAnyCreateRequest(CreateServiceInstanceRequest request) {
		MaintenanceInfo catalogMi = request.getPlan().getMaintenanceInfo();
		MaintenanceInfo requestedMi = request.getMaintenanceInfo();
		if (requestedMi != null && ! requestedMi.equals(catalogMi)) {
			throw new ServiceBrokerMaintenanceInfoConflictException("unknown requested maintenance info: " + requestedMi + " Currently supported maintenance info is: " + catalogMi);
		}
	}

	private String formatBuildMetadataSuffix(Version cmdbVersion) {
		return "osb-cmdb."
			+ cmdbVersion.getMajor() + "."
			+ cmdbVersion.getMinor() + "."
			+ cmdbVersion.getPatch()
			+ (cmdbVersion.getBuildMetaData().isEmpty() ? "" : "." + cmdbVersion.getBuildMetaData());
	}

	protected MaintenanceInfo unmergeInfos(MaintenanceInfo brokeredMaintenanceInfo) {
		Version backendVersion = Version.parseVersion(brokeredMaintenanceInfo.getVersion());
		Version cmdbVersion = Version.parseVersion(osbCmdbMaintenanceInfo.getVersion());

		String extension = StringUtils.removeEnd(backendVersion.getBuildMetaData(),
			formatBuildMetadataSuffix(cmdbVersion));
		extension = StringUtils.removeEnd(extension, ".");
		extension = StringUtils.removeEnd(extension, cmdbVersion.getBuildMetaData());
		if (! extension.isEmpty()) {
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

}
