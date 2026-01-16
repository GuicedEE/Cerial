package com.guicedee.cerial.enumerations;

import java.util.EnumSet;

/**
 * Declares the intended usage of a serial port connection.
 */
public enum ComPortType
{
    /** Physical scanner device. */
    Scanner,
    /** Generic serial device. */
    Device,
    /** Server-side serial endpoint. */
    Server,
    /** Printer using PPLA protocol. */
    PrinterPPLA,
    /** Printer using PPLB protocol. */
    PrinterPPLB,
    /** Printer using PPLZ protocol. */
    PrinterPPLZ,
    /** Simulated scanner device. */
    ScannerSim,
    /** Simulated generic device. */
    DeviceSim;

    /** Device + server + simulation types (used for broad matching). */
    public static final EnumSet<ComPortType> deviceServerSim = EnumSet.of(ComPortType.Server,ComPortType.Device,DeviceSim);
    /** Device + server types (no simulation). */
    public static final EnumSet<ComPortType> deviceServer = EnumSet.of(ComPortType.Server,ComPortType.Device);
    /** Device simulation type only. */
    public static final EnumSet<ComPortType> deviceSim = EnumSet.of(DeviceSim);

    /** Physical scanners only. */
    public static final EnumSet<ComPortType> scanners = EnumSet.of(Scanner);
    /** Simulated scanners only. */
    public static final EnumSet<ComPortType> scannerSim = EnumSet.of(ScannerSim);
    /** Physical and simulated scanners. */
    public static final EnumSet<ComPortType> scannersAndSim = EnumSet.of(Scanner,ScannerSim);

    /** All supported printer types. */
    public static final EnumSet<ComPortType> printers = EnumSet.of(PrinterPPLA,PrinterPPLB,PrinterPPLZ);
}
