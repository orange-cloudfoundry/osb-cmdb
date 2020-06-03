package com.orange.oss.osbcmdb.serviceinstance;

import de.skuzzle.semantic.Version;
import org.apache.commons.lang3.StringUtils;
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

	private String formatBuildMetadataSuffix(Version cmdbVersion) {
		return "osb-cmdb."
			+ cmdbVersion.getMajor() + "."
			+ cmdbVersion.getMinor() + "."
			+ cmdbVersion.getPatch()
			+ (cmdbVersion.getBuildMetaData().isEmpty() ? "" : "." + cmdbVersion.getBuildMetaData());
	}

	public MaintenanceInfo unmergeInfos(MaintenanceInfo brokeredMaintenanceInfo) {
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
