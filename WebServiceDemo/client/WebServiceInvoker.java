
package com.webservice.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
//FIXME: CommonLogger implementation required
/**
 * <p>Title: WebServiceInvoker </p>
 * <p>Description: Client which will be used to Call REST APIs 
 */
public class WebServiceInvoker {

	public static final String HOST_PATTERN = "<HOST>";

	public static final String PARAMETER_PATTERN = "\\$.";//$1

	public static final String CHARSET = "UTF-8";
	
	private String Default_HTTPS_Timeout = "300000";

	private String XML_Content_Type = "application/xml";

	private void setCommonRequestHeaders(HttpsURLConnection connection) {
		String self = "setCommonRequestHeaders:";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");
		if (connection != null) {
			/*BASE64Encoder encoder = new BASE64Encoder();
			String encodedCredential = encoder.encode((userName + ":" + password).getBytes());

			connection.setRequestProperty("Authorization", "Basic " + encodedCredential);
			 */
			connection.setRequestProperty("User-Agent", "Apache-HttpClient/4.1.1 (java 1.5)");
			connection.setRequestProperty("Accept-CHARSET", CHARSET);
			connection.setRequestProperty("Accept", "application/xml, text/plain, text/xml, text/html, image/gif, image/jpeg");
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setAllowUserInteraction(false);

			int TimeOut = Integer.parseInt(Default_HTTPS_Timeout.trim());
			connection.setConnectTimeout(TimeOut);
		}
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit.");
	}

	private void setRequestIOForPostAndPut(HttpsURLConnection connection) {
		if (connection != null) {
			connection.setDoOutput(true);
		}
	}

	private void setRequestHeadersForFileTransfer(HttpsURLConnection connection, File sourceFile) {
		String self = "setRequestHeadersForFileTransfer:";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		if (connection != null && sourceFile != null) {
			CommonLogger.info(LoggerTypes.DEBUG, self + "connection:" + connection);
			connection.setRequestProperty("Content-Type", "multipart/form-data");//boundary??
			connection.setRequestProperty("Content-Length",
					"" + Long.toString(14 + 65 + sourceFile.getName().length() + 24 + sourceFile.length() + 18));

		}
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit.");
	}

	private void setRequestHeadersForXmlPayload(HttpsURLConnection connection, String xmlPayload) {
		if (connection != null && xmlPayload != null) {
			connection.setRequestProperty("Content-Type", XML_Content_Type + "; CHARSET=" + CHARSET);//text/xml
			connection.setRequestProperty("Content-Length", xmlPayload.length() + "");
		}
	}

	private void setContentTypeForGetAndDelete(HttpsURLConnection connection) {
		if (connection != null) {
			connection.setRequestProperty("Content-Type",XML_Content_Type + /*"text/xml;*/ "; CHARSET=" + CHARSET);//TODO CHeck
			connection.setRequestProperty("Content-Length", "0");
		}
	}

	private void flushXmlPayload(HttpsURLConnection connection, String xmlPayload) throws Exception {
		String self = "flushXmlPayload:";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		CommonLogger.info(LoggerTypes.DEBUG, self + " Inputs : connection:" + connection + " xmlPayload:" + xmlPayload);
		
		OutputStream dataOut = null;
		PrintWriter writer = null;
		if (connection != null && xmlPayload != null) {
			try {
				dataOut = connection.getOutputStream();
				writer = new PrintWriter(new OutputStreamWriter(dataOut, CHARSET), true); // PrintWriter Auto Flush mode
				writer.println(xmlPayload);
				dataOut.flush();// may be not required
			} catch (Exception e) {
				CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
				throw e;
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch (Exception e) {
					CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
				}
			}
		}
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit.");
	}

	private void readFileResponse(HttpsURLConnection connection, File recivedFile) throws Exception {
		String self = "readFileResponse():";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		if (connection != null && (connection.getResponseCode() == HttpsURLConnection.HTTP_OK)) {
			CommonLogger.info(LoggerTypes.DEBUG, self + "Response code:" + connection.getResponseCode());
			CommonLogger.info(LoggerTypes.DEBUG, self + " Inputs : connection:" + connection + " recivedFile:"
					+ recivedFile);
			if (recivedFile != null) {
				BufferedOutputStream bos = null;
				BufferedInputStream bis = null;
				try {
					bos = new BufferedOutputStream(new FileOutputStream(recivedFile));
					bis = new BufferedInputStream(connection.getInputStream());
					byte[] data = new byte[1024];
					int bytesRead = 0;
					while ((bytesRead = bis.read(data)) != -1) {
						bos.write(data, 0, bytesRead);
					}
					bos.flush();
				} finally {
					try {
						if (bos != null) {
							bos.close();
						}
					} catch (Exception e1) {
						CommonLogger.info(LoggerTypes.DEBUG, "Static Block: Error closing the stream : "/*, e1*/);
					}
					try {
						if (bis != null) {
							bis.close();
						}
					} catch (Exception e1) {
						CommonLogger.error(LoggerTypes.DEBUG, "Static Block: Error closing the stream : "/*, e1*/);
					}
				}
			} else {
				CommonLogger.error(LoggerTypes.DEBUG, self + "Received file null.");
			}
		} else {
			if (connection != null) {
				CommonLogger.error(LoggerTypes.DEBUG,
						self + connection.getResponseCode() + ":" + connection.getResponseMessage());
				throw new WebserviceClientException(connection.getResponseCode() + ":" + connection.getResponseMessage());
			}
		}
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit.");
	}

	private StringBuffer readXmlResponse(HttpsURLConnection connection) throws Exception {
		String self = "readXmlResponse():";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");
		StringBuffer xmlResponse = new StringBuffer();

		if (connection != null && (connection.getResponseCode() == HttpsURLConnection.HTTP_OK)) {
			CommonLogger.info(LoggerTypes.DEBUG, self + "Response code:" + connection.getResponseCode());

			BufferedReader responseReader = null;
			try {
				char buffer[] = new char[1024];
				responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				int charsRead = 0;
				while ((charsRead = responseReader.read(buffer)) != -1) {
					xmlResponse.append(new String(buffer, 0, charsRead));
				}

			} catch (Exception e) {
				CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
				throw e;
			} finally {
				try {
					if (responseReader != null) {
						responseReader.close();
					}
				} catch (Exception e) {
					CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
				}
			}
		} else if (connection != null && (connection.getResponseCode() == HttpsURLConnection.HTTP_CREATED)) {
			BufferedReader responseReader = null;

			try {
				char buffer[] = new char[1024];
				responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				int charsRead = 0;
				while ((charsRead = responseReader.read(buffer)) != -1) {
					xmlResponse.append(new String(buffer, 0, charsRead));
				}

			} catch (Exception e) {
				CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
				throw e;
			} finally {
				try {
					if (responseReader != null) {
						responseReader.close();
					}
				} catch (Exception e) {
					CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
				}
			}

		} else {
			if (connection != null) {
				//read error stream if response code is not 2xx
				BufferedReader responseReader = null;

				try {
					char buffer[] = new char[1024];
					responseReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
					int charsRead = 0;
					while ((charsRead = responseReader.read(buffer)) != -1) {
						xmlResponse.append(new String(buffer, 0, charsRead));
					}
					CommonLogger.info(LoggerTypes.DEBUG, self + " Inputs : xmlResponse:" + xmlResponse);

				} catch (Exception e) {
					CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
				} finally {
					try {
						if (responseReader != null) {
							responseReader.close();
						}
					} catch (Exception e) {
						CommonLogger.error(LoggerTypes.DEBUG, e.getMessage());
					}
				}

				CommonLogger.error(LoggerTypes.DEBUG,
						self + connection.getResponseCode() + ":" + connection.getResponseMessage() + ".Error: " + xmlResponse);
				throw new WebserviceClientException(connection.getResponseCode() + ":" + connection.getResponseMessage()
						+ ".Error: " + xmlResponse);
			}

		}
		CommonLogger.info(LoggerTypes.DEBUG, self + " Inputs : xmlResponse:" + xmlResponse);
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit.");
		return xmlResponse;
	}

	private String modifyUri(String uri, String ip, String... params) throws Exception {
		String modifiedUri = null;
		String self = "modifyUri():";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");
		if (uri != null && ip != null) {
			modifiedUri = uri.replace(HOST_PATTERN, ip);

			Pattern pattern = Pattern.compile(PARAMETER_PATTERN);
			Matcher match = pattern.matcher(modifiedUri);

			if (match.find())//check if it contains regex pattern
			{
				if (params != null && params.length != 0) {
					if (params.length == 1) {
						//replaces using regex specified
						modifiedUri = modifiedUri.replaceFirst(PARAMETER_PATTERN,
								URLEncoder.encode(params[0], CHARSET));
					} else {
						for (String p : params) {
							//replaces first token as per regex pattern 
							modifiedUri = modifiedUri.replaceFirst(PARAMETER_PATTERN,
									URLEncoder.encode(p, CHARSET));
						}
					}
				}
			}//else return url as it is
			CommonLogger.info(LoggerTypes.DEBUG, self + "URI formed: " + modifiedUri);
		} else {
			CommonLogger.error(LoggerTypes.DEBUG, self + "IP is required");
			throw new WebserviceClientException("IP is required");
		}
		CommonLogger.info(LoggerTypes.DEBUG, self + "modifiedUri:" + modifiedUri);
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit.");
		return modifiedUri;
	}

	private String invokeWebService(String method, String target, File sourceFile, String xmlPayload) throws Exception {
		String self = "invokeWebService():";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		CommonLogger.info(LoggerTypes.DEBUG, self + " Inputs : method:" + method + " target:" + target
				+ " xmlPayload:" + xmlPayload);
		if (method == null || target == null) {
			CommonLogger.error(LoggerTypes.DEBUG, self + "Insufficient parameters.");
			throw new WebserviceClientException("Insufficient parameters");
		}

		URL url = new URL(target);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		StringBuffer responseBody = new StringBuffer();

		try {
			CommonLogger.info(LoggerTypes.DEBUG, self + "connection:" + connection);
			setCommonRequestHeaders(connection);

			connection.setRequestMethod(method);
			if ("POST".equals(method) || "PUT".equals(method)) {
				setRequestIOForPostAndPut(connection);
				if (sourceFile != null) {//TODO:check if this section is reqd 
					setRequestHeadersForFileTransfer(connection, sourceFile);
					//flushFile(connection, sourceFile);-TODO check if reqd
				} else if (xmlPayload != null) {
					setRequestHeadersForXmlPayload(connection, xmlPayload);
					flushXmlPayload(connection, xmlPayload);
				} else {
					// POST or PUT without any xmlPayload or sourceFile
					setContentTypeForGetAndDelete(connection);
					connection.connect();
				}
				responseBody = readXmlResponse(connection);
			} else if ("GET".equals(method) || "DELETE".equals(method)) {
				setContentTypeForGetAndDelete(connection);
				connection.connect();
				if (sourceFile != null) {
					if ("GET".equals(method)) {
						//its a get file request, expect a file to be received 
						readFileResponse(connection, sourceFile);
						responseBody.append("File Returned");
					}
				} else {
					//Used for SM webservice call
					//expecting a xml response
					responseBody = readXmlResponse(connection);
				}
			}
		} finally {
			connection.disconnect();
		}
		CommonLogger.info(LoggerTypes.DEBUG, self + "responseBody.toString():" + responseBody.toString());
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit.");
		return responseBody.toString();

	}

	public String performWSCall(String method, String uri, String hostIP, String xmlPayload, File sourceFile, String... params)
			throws Exception {
		
		String self = "performWSCall():";
		CommonLogger.info(LoggerTypes.DEBUG, self + "Entered ");
		String result = null;

		if (method != null && uri != null && hostIP != null) {

			String uriModified = null;

			//check if pattern exists in uri
			Pattern pattern = Pattern.compile(PARAMETER_PATTERN);
			Matcher match = pattern.matcher(uri);

			if (match.find()) {
				//parameters required
				if (params != null && params.length != 0) {
					uriModified = modifyUri(uri, hostIP, params);
				} else {
					CommonLogger.info(LoggerTypes.DEBUG, self + "No parameter specified.");
					throw new WebserviceClientException("No parameter(s) specified in URI.");
				}
			} else {
				//doesnot contain any params to be filled
				uriModified = modifyUri(uri, hostIP, new String[] {});
			}

			CommonLogger.info(LoggerTypes.DEBUG, self + "URI sent for request: " + uriModified);
			result = invokeWebService(method, uriModified, sourceFile, xmlPayload);

		} else {
			CommonLogger.error(LoggerTypes.DEBUG, self + "Bad action or not specified.");
			throw new WebserviceClientException("Bad action or not specified");
		}
		CommonLogger.info(LoggerTypes.DEBUG, self + "Exit with result " + result);
		return result;
	}
}
