package com.guicedee.cerial.implementations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListenerWithExceptions;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.google.common.base.Strings;
import com.guicedee.cerial.CerialPortConnection;
import com.guicedee.cerial.SerialPortException;
import com.guicedee.cerial.enumerations.ComPortStatus;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScopeSource;
import com.guicedee.client.utils.LogUtils;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.core.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fazecast.jSerialComm.SerialPort.*;
import static com.guicedee.cerial.CerialPortConnection.portNumberFormat;
import static com.guicedee.cerial.enumerations.ComPortStatus.Running;

@Getter
@Setter
public class DataSerialPortBytesListener implements SerialPortDataListenerWithExceptions, ComPortEvents
{
		@JsonIgnore
		private Logger log;
		@JsonIgnore
		private BiConsumer<byte[], SerialPort> comPortRead;
		
		@JsonIgnore
		private SerialPort comPort;
		@JsonIgnore
		private CerialPortConnection<?> connection;
		
		@JsonIgnore
		private char[] delimiter;
		
		@JsonIgnore
		private Pattern patternMatch;
		
		private Mode mode = Mode.Delimeter;
		
		private int maxBufferLength = 1024;
		
		private Set<Character> allowedChars = new java.util.HashSet<>();
		
		public enum Mode
		{
				Delimeter,
				Pattern,
				Length,
				All
		}
		
		public DataSerialPortBytesListener(char[] delimiter, SerialPort comPort, CerialPortConnection<?> connection)
		{
				this.delimiter = delimiter;
				this.comPort = comPort;
				this.connection = connection;
				String loggerName = (connection.getComPort() == 0) ? "cerial" : "COM" + connection.getComPort();
				log = LogUtils.getSpecificRollingLogger(loggerName, "cerial",
																																												"[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%-5level] - [%msg]%n", false);
		}
		
		@Override
		public int getListeningEvents()
		{
				return LISTENING_EVENT_DATA_RECEIVED | LISTENING_EVENT_PORT_DISCONNECTED | LISTENING_EVENT_BREAK_INTERRUPT | LISTENING_EVENT_FRAMING_ERROR | LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR | LISTENING_EVENT_PARITY_ERROR | LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR;
		}
		
		public byte[] remove(byte[] array, byte toRemove)
		{
				List<Byte> byteList = new ArrayList<>(Arrays.asList(ArrayUtils.toObject(array)));
				byteList.removeIf(b -> b == toRemove);
				return ArrayUtils.toPrimitive(byteList.toArray(new Byte[0]));
		}
		
		@Override
		public void serialEvent(SerialPortEvent event)
		{
				if (event.getEventType() == LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR)
				{
						log.error("❌ Software Overrun Error: {}", event.toString());
						connection.onConnectError(new SerialPortException("Software Overrun Error - " + event.toString()), ComPortStatus.GeneralException);
				}
				else if (event.getEventType() == LISTENING_EVENT_PARITY_ERROR)
				{
						log.error("❌ Software Parity Error: {}", event.toString());
						connection.onConnectError(new SerialPortException("Software Parity Error - " + event.toString()), ComPortStatus.GeneralException);
				}
				else if (event.getEventType() == LISTENING_EVENT_FRAMING_ERROR)
				{
						log.error("❌ Hardware Framing Error: {}", event.toString());
						connection.onConnectError(new SerialPortException("Hardware Framing Error - " + event.toString()), ComPortStatus.GeneralException);
				}
				else if (event.getEventType() == LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR)
				{
						log.error("❌ Hardware Firmware Overrun Error: {}", event.toString());
						connection.onConnectError(new SerialPortException("Hardware Firmware Overrun Error - " + event.toString()), ComPortStatus.GeneralException);
				}
				else if (event.getEventType() == LISTENING_EVENT_BREAK_INTERRUPT)
				{
						log.error("❌ Hardware Break Interrupt Error: {}", event.toString());
						connection.onConnectError(new SerialPortException("Hardware Break Interrupt Error - " + event.toString()), ComPortStatus.GeneralException);
				}
				else if (event.getEventType() == LISTENING_EVENT_PORT_DISCONNECTED)
				{
						log.error("🔌 Port disconnected: {}", event.toString());
						connection.onConnectError(new SerialPortException("Port disconnected - " + event.toString()), ComPortStatus.Offline);
				}
				else if (event.getEventType() == LISTENING_EVENT_DATA_RECEIVED)
				{
						byte[] newData = event.getReceivedData();
						processReceivedBytes(newData);
				}
		}
		
		private StringBuilder buffer = new StringBuilder();
		
		public void processReceivedBytes(byte[] newData)
		{
				newData = remove(newData, (byte) 0);
				String message = "";
				
				Set<Character> delChars = new java.util.HashSet<>();
				for (char c : delimiter)
				{
						delChars.add(c);
				}
				for (byte b : newData)
				{
						if (b == 0)
						{
								continue;
						}
						Character c = (char) b;
						if ((!allowedChars.isEmpty() && !allowedChars.contains((char) b)) && (delimiter.length > 0 && !delChars.contains(c)))
						{
								log.warn("⚠️ Character not allowed on serial port - Port [{}] - Character [{}] - Resetting buffer", getConnection().getComPort(), (char) b);
								buffer = new StringBuilder();
								continue;
						}
						
						// Common append logic
						if (buffer.length() >= maxBufferLength)
						{
								log.warn("⚠️ Buffer limit reached on serial port - Port [{}] - Rolling data", getConnection().getComPort());
								buffer.deleteCharAt(0);
						}
						buffer.append((char) b);
						
						boolean messageProcessed = false;
						
						// 1. Check Pattern
						if ((mode == Mode.All || mode == Mode.Pattern) && patternMatch != null)
						{
								Matcher matcher = patternMatch.matcher(buffer.toString());
								if (matcher.find())
								{
										message = matcher.group();
										try
										{
												if (!Strings.isNullOrEmpty(message))
												{
														log.info("📥 RX] - Port [{}] - Message: [{}]", portNumberFormat.format(connection.getComPort()), message);
														processMessage(message.getBytes());
														messageProcessed = true;
												}
										}
										catch (Throwable e)
										{
												log.error("❌ Error processing received message: {}", e.getMessage(), e);
										}
										buffer = new StringBuilder(buffer.substring(matcher.end()));
								}
						}
						
						// 2. Check Delimiter (if not already processed by pattern)
						if (!messageProcessed && (mode == Mode.All || mode == Mode.Delimeter))
						{
								boolean foundDelimiter = false;
								for (char delimiterCheck : delimiter)
								{
										if (delimiterCheck == b)
										{
												foundDelimiter = true;
												break;
										}
								}
								
								if (foundDelimiter)
								{
										// Message is everything in buffer minus the delimiter (optional, depending on how it was handled before)
										// Looking at previous implementation, it included everything in buffer.
										message = buffer.toString();
										try
										{
												log.info("RX] - [" + portNumberFormat.format(connection.getComPort()) + "] - [" + message.trim());
												processMessage(message.getBytes());
												messageProcessed = true;
										}
										catch (Throwable e)
										{
												log.error(e.getMessage(), e);
										}
										buffer = new StringBuilder();
								}
						}
						
						// 3. Check Length (if not already processed)
						if (!messageProcessed && (mode == Mode.All || mode == Mode.Length))
						{
								if (buffer.length() >= maxBufferLength)
								{
										message = buffer.toString();
										try
										{
												log.info("RX] - [" + portNumberFormat.format(connection.getComPort()) + "] - [" + message.trim());
												processMessage(message.getBytes());
												messageProcessed = true;
										}
										catch (Throwable e)
										{
												log.error(e.getMessage(), e);
										}
										buffer = new StringBuilder();
								}
						}
				}
				
		}
		
		private void processMessage(byte[] newData)
		{
				try
				{
						var vertx = IGuiceContext.get(Vertx.class);
						vertx.executeBlocking(() -> {
								com.guicedee.client.scopes.CallScoper callScoper = null;
								boolean started = false;
								try
								{
										callScoper = IGuiceContext.get(com.guicedee.client.scopes.CallScoper.class);
										if (!callScoper.isStartedScope())
										{
												callScoper.enter();
												started = true;
										}
										CallScopeProperties properties = IGuiceContext.get(CallScopeProperties.class);
										if (properties.getSource() == null || properties.getSource() == CallScopeSource.Unknown)
										{
												properties.setSource(CallScopeSource.SerialPort);
										}
										properties
											.getProperties()
											.put("ComPort", comPort);
										properties
											.getProperties()
											.put("CerialPortConnection", this);
										getConnection().setComPortStatus(Running);
										if (comPortRead != null)
										{
												try
												{
														comPortRead.accept(newData, comPort);
												}
												catch (Throwable e)
												{
														log.fatal("Fatal error in ComPortRead handler on ComPort [{}]: {}", connection.getComPort(), e.getMessage(), e);
												}
										}
								}
								catch (Throwable T)
								{
										log.error("Error on ComPort [" + connection.getComPort() + "] Receipt", T);
								}
								finally
								{
										if (started && callScoper != null)
										{
												callScoper.exit();
										}
								}
								return true;
						}, false);
				}
				catch (Exception e)
				{
						log.error("Error on running bytes serial ComPort [" + connection.getComPort() + "] Receipt", e);
				}
		}
		
		@Override
		public void catchException(Exception e)
		{
				log.error("❌ Error on ComPort [{}] Receipt: {}", connection.getComPort(), e.getMessage(), e);
		}
}
