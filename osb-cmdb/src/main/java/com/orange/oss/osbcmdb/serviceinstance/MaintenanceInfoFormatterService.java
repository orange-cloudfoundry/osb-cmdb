package com.orange.oss.osbcmdb.serviceinstance;

import de.skuzzle.semantic.Version;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class MaintenanceInfoFormatterService {

	private MaintenanceInfo osbCmdbMaintenanceInfo;
	protected final Logger LOG = Loggers.getLogger(OsbCmdbServiceInstance.class);

	public MaintenanceInfoFormatterService(MaintenanceInfo osbCmdbMaintenanceInfo) {
		this.osbCmdbMaintenanceInfo = osbCmdbMaintenanceInfo;
	}

	public MaintenanceInfo formatForCatalog(MaintenanceInfo backendCatalogMaintenanceInfo) {
		if (osbCmdbMaintenanceInfo == null) {
			return backendCatalogMaintenanceInfo;
		}
		if (backendCatalogMaintenanceInfo != null) {
			return mergeInfos(backendCatalogMaintenanceInfo);
		}
		return osbCmdbMaintenanceInfo;
	}

	public MaintenanceInfo formatForInstance(MaintenanceInfo backendCatalogMaintenanceInfo,
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

	public MaintenanceInfo mergeInfos(MaintenanceInfo backendMaintenanceInfo) {
		Version backendVersion = Version.parseVersion(backendMaintenanceInfo.getVersion());
		Version cmdbVersion = Version.parseVersion(osbCmdbMaintenanceInfo.getVersion());
		String backendBuildMetaData = backendVersion.getBuildMetaData();
		String buildMetaData = "+";
		if (! backendBuildMetaData.isEmpty()) {
			buildMetaData = buildMetaData+ backendBuildMetaData + ".";
		}
		buildMetaData = buildMetaData + "osb-cmdb."
			+ cmdbVersion.getMajor() + "."
			+ cmdbVersion.getMinor() + "."
			+ cmdbVersion.getPatch()
			+ (cmdbVersion.getBuildMetaData().isEmpty() ? "" : "." + cmdbVersion.getBuildMetaData());
		String description = defaultString(backendMaintenanceInfo.getDescription());
		if (osbCmdbMaintenanceInfo.getDescription() != null) {
			description = description + "\n" + osbCmdbMaintenanceInfo.getDescription();
		}
		return MaintenanceInfo.builder()
			.version(backendVersion.getMajor(), backendVersion.getMinor(), backendVersion.getPatch(),
				buildMetaData)
			.description(description)
			.build();
	}

}
