package com.guicedee.cerial.enumerations;

import lombok.Getter;

import java.util.EnumSet;

/**
 * Represents the operational status of a serial port connection along with UI metadata.
 */
@Getter
public enum ComPortStatus
{
	/** Error state for unhandled or unexpected exceptions. */
	GeneralException("exclamation-circle", "bg-danger", "text-danger"),
	/** Port is missing or not detected. */
	Missing("engine-warning", "bg-danger", "text-danger"),
	/** Connection attempt failed. */
	Failed("engine-warning", "bg-danger", "text-danger"),
	/** Port is already in use by another process. */
	InUse("wifi-slash", "bg-danger", "text-danger"),
	/** Port is closed or disconnected. */
	Offline("map-marker-alt-slash", "bg-secondary", "text-secondary"),
	/** Port is busy with a long-running operation. */
	OperationInProgress("spinner", "bg-purple", "text-purple"),
	/** Port is transferring a file or bulk data. */
	FileTransfer("exchange-alt", "bg-purple", "text-purple"),
	/** Port is running in a simulated mode. */
	Simulation("galaxy", "bg-purple", "text-purple"),
	/** Port is opening or initializing. */
	Opening("outlet", "bg-info", "text-info"),
	/** Port is connected but idle. */
	Silent("lightbulb-exclamation", "bg-warning", "text-warning"),
	/** Port is connected and logging telemetry. */
	Logging("wind-turbine", "bg-info", "text-info"),
	/** Port is connected and actively running. */
	Running("lightbulb-on", "bg-success", "text-success");
	
	/** UI icon name for this status. */
	private String icon;
	
	/** UI background class for this status. */
	private String backgroundClass;
	
	/** UI foreground/text class for this status. */
	private String foregroundClass;
	
	/** Statuses that represent exceptional or offline states. */
	public static final EnumSet<ComPortStatus> exceptionOperations = EnumSet.of(GeneralException,Missing,InUse,Offline) ;
	/** Statuses that indicate temporary pause or transitional states. */
	public static final EnumSet<ComPortStatus> pauseOperations = EnumSet.of(OperationInProgress,FileTransfer,Opening) ;
	/** Statuses that count as active in the UI. */
	public static final EnumSet<ComPortStatus> portActive = EnumSet.of(Silent, Logging, Running, Simulation);
	/** Statuses that count as fully offline in the UI. */
	public static final EnumSet<ComPortStatus> portOffline = EnumSet.of(Offline);

	/** Statuses considered online or servicing for idle monitoring. */
	public static final EnumSet<ComPortStatus> onlineServerStatus = EnumSet.of(Simulation, Opening, Logging, OperationInProgress, Running, Silent, FileTransfer);

	/**
	 * Constructs a status with UI metadata.
	 *
	 * @param icon            the icon identifier
	 * @param backgroundClass the background CSS class
	 * @param foregroundClass the foreground CSS class
	 */
	ComPortStatus(String icon, String backgroundClass, String foregroundClass)
	{
		this.icon = icon;
		this.backgroundClass = backgroundClass;
		this.foregroundClass = foregroundClass;
	}

	/**
	 * Updates the icon identifier.
	 *
	 * @param icon the icon identifier
	 * @return this status for fluent updates
	 */
    public ComPortStatus setIcon(String icon)
	{
		this.icon = icon;
		return this;
	}

	/**
	 * Updates the background CSS class.
	 *
	 * @param backgroundClass the background class name
	 * @return this status for fluent updates
	 */
    public ComPortStatus setBackgroundClass(String backgroundClass)
	{
		this.backgroundClass = backgroundClass;
		return this;
	}

	/**
	 * Updates the foreground CSS class.
	 *
	 * @param foregroundClass the foreground class name
	 * @return this status for fluent updates
	 */
    public ComPortStatus setForegroundClass(String foregroundClass)
	{
		this.foregroundClass = foregroundClass;
		return this;
	}
}
