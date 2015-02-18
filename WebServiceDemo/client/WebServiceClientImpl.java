package com.webservice.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.webservice.client.WebServiceInvoker;
import com.webservice.client.exceptions.WebserviceClientException;
import com.model.Location;
import com.model.LocationPattern;

public class WebServiceClientImpl {

	private static String HostIP = "localhost:443";//since  webservice is within same container

	/*static {
		try {
			InetAddress local = InetAddress.getLocalHost();
			HostIP = local.getHostAddress();
		} catch (Exception e) {
			CommonLogger.info(LoggerTypes.DEBUG, "WebServiceImpl static block ():" + e.getMessage(), e);
		}
	}*/

	public static String getAllLocations() {
		String result = null;
		String self = "WebServiceClientImpl.getAllLocations(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		try {
			result = new WebServiceInvoker().performWSCall(WSConstants.METHOD_GET, WSConstants.URI_ALL_LOCATIONS,
					HostIP, null, null, new String[] {});

			CommonLogger.info(LoggerTypes.DEBUG, self + " result \n: " + result);

		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, "getAllLocations():" + e.getMessage(), e);
		}

		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return result;
	}

	public static String getLocation(String key, String... params) {
		String result = null;
		String self = "WebServiceClientImpl.getLocation(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		try {
			String uri = WSConstants.URI_SINGLE_LOCATION;
			if (key.equals(WSConstants.KEY_LOCATION_NAME)) {
				uri = WSConstants.URI_SINGLE_LOCATION_BY_NAME;
			} else if (key.equals(WSConstants.KEY_CORRELATION)) {
				uri = WSConstants.URI_SINGLE_LOCATION_BY_CORRELATION;
			}

			result = new WebServiceInvoker().performWSCall(WSConstants.METHOD_GET, uri, HostIP, null, null, params);
			
		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage(), e);
		}

		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return result;
	}

	/**
	 * Adding  location using mandatory  parameters
	 * 
	 * @param loc_name
	 * @param correlationId
	 * @param locationPatterns
	 * @return
	 * @throws WebserviceClientException
	 */
	public static String addLocation(String loc_name, String correlationId, List<String> locationPatterns)
			throws WebserviceClientException {
		String result = null;
		String self = "WebServiceClientImpl.addLocation(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");
		String xmlResponse = null;
		try {
			//create  location data xml
			if (loc_name == null || correlationId == null) {
				throw new WebserviceClientException("Location Name or correlation found null");
			}
			String xmlRequest = createLocationXml(loc_name, correlationId, locationPatterns);

			CommonLogger.info(LoggerTypes.DEBUG, self + " xml Request sent to ws : " + xmlRequest);

			xmlResponse = new WebServiceInvoker().performWSCall(WSConstants.METHOD_POST, WSConstants.URI_ALL_LOCATIONS,
					HostIP, xmlRequest, null, new String[] {});

		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage(), e);
			String errorMSG = null;
			if (e.getMessage() != null){
				errorMSG = findKeyValueInXml(WSConstants.KEY_ERROR_MSG,  e.getMessage().substring(e.getMessage().indexOf("Error:")+6).trim());
			}
			if (errorMSG != null) {
				errorMSG = WSConstants.WS_ERRORMESSAGE_PREFIX + " error - " + errorMSG;
				throw new WebserviceClientException(errorMSG);
			} else {
				throw new WebserviceClientException(e);
			}			
		}

		if (xmlResponse == null) {
			CommonLogger.error(LoggerTypes.DEBUG, self + "Null response from  Location webservice.");
			throw new WebserviceClientException(WSConstants.NULL_RESPONSE_ERRORMESSAGE);
		} else {
			CommonLogger.info(LoggerTypes.DEBUG, self + "xmlResponse:" + xmlResponse);

			Location loc = extractLocationFromXml(xmlResponse);
			if (loc != null) {
				CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML Name:" + loc.getName());
				CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML Correlation:" + loc.getCorrelation());
				CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML Pattern :");
				if(loc.getLocationPatterns()!=null){
					for (LocationPattern locpat : loc.getLocationPatterns()) {
						CommonLogger.info(LoggerTypes.DEBUG, self + "pattern: " + locpat.getIpaddresspattern() + " link: "
								+ locpat.getLink());
					}
				}
				if (loc.getLink() != null && loc.getLink().getHref() != null) {
					String hrefLink = loc.getLink().getHref().toString();
					CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML - Self Link:" + hrefLink);
					result = getSurrogateIdFromLink(hrefLink);
				} else {
					throw new WebserviceClientException("No Location ID from Add webservice.");
				}

			} else {
				CommonLogger.error(LoggerTypes.DEBUG, self + " Could not generate location data or add failed. ");
				String errorMSG = findKeyValueInXml(WSConstants.KEY_ERROR_MSG, xmlResponse);
				if (errorMSG != null) {
					errorMSG = WSConstants.WS_ERRORMESSAGE_PREFIX + " error - " + errorMSG;
					throw new WebserviceClientException(errorMSG);
				} else {
					throw new WebserviceClientException(xmlResponse);
				}
			}
		}
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return result;
	}

	public static String updateLocation(String surrogateId, String loc_name, String correlationId, List<String> locationPatterns)
			throws WebserviceClientException {
		String result = null;
		String self = "WebServiceClientImpl.updateLocation(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");
		String xmlResponse = null;

		try {
			if (locationPatterns != null && locationPatterns.size() > 0) {

				//create xml
				String xmlRequest = createLocationXml(loc_name, correlationId, locationPatterns);

				//call webservice with xmlpPayload	
				CommonLogger.info(LoggerTypes.DEBUG, self + " xmlRequest sent to ws : " + xmlRequest);

				xmlResponse = new WebServiceInvoker().performWSCall(WSConstants.METHOD_PUT,
						WSConstants.URI_SINGLE_LOCATION, HostIP, xmlRequest, null, surrogateId);

			} else {
				//use matrix call bservice
				xmlResponse = new WebServiceInvoker()
						.performWSCall(WSConstants.METHOD_POST, WSConstants.URI_EDIT_LOCATION_NAME_AND_CORREL, HostIP, null,
								null, surrogateId, loc_name, correlationId);
			}

			CommonLogger.info(LoggerTypes.DEBUG, self + "result:" + xmlResponse);

		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage(), e);
			String errorMSG = null;
			if (e.getMessage() != null) {
				errorMSG = findKeyValueInXml(WSConstants.KEY_ERROR_MSG, e.getMessage().substring(e.getMessage().indexOf("Error:") + 6).trim());
			}
			if (errorMSG != null) {
				errorMSG = WSConstants.WS_ERRORMESSAGE_PREFIX + " error - " + errorMSG;
				throw new WebserviceClientException(errorMSG);
			} else {
				throw new WebserviceClientException(e);
			}
		}

		if (xmlResponse == null) {
			CommonLogger.error(LoggerTypes.DEBUG, self + "Null response from webservice");
			throw new WebserviceClientException(WSConstants.NULL_RESPONSE_ERRORMESSAGE);
		} else {
			CommonLogger.info(LoggerTypes.DEBUG, self + "xmlResponse:" + xmlResponse);

			Location loc = extractLocationFromXml(xmlResponse);
			if (loc != null) {
				CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML Name:" + loc.getName());
				CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML Correlation:" + loc.getCorrelation());
				CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML Pattern :");
				if(loc.getLocationPatterns()!=null){
					for (LocationPattern locpat : loc.getLocationPatterns()) {
						CommonLogger.info(LoggerTypes.DEBUG, self + "pattern: " + locpat.getIpaddresspattern() + " link: "
								+ locpat.getLink());
					}
				}

				if (loc.getLink() != null && loc.getLink().getHref() != null) {
					String hrefLink = loc.getLink().getHref().toString();
					CommonLogger.info(LoggerTypes.DEBUG, self + "Location from XML  Location Surrogate Id:"
							+ hrefLink);
					result = getSurrogateIdFromLink(hrefLink);

				} else {
					throw new WebserviceClientException("No Location ID from webservice on update.");
				}

			} else {
				CommonLogger.error(LoggerTypes.DEBUG, self + " Could not generate location data or update failed. ");
				String errorMSG = findKeyValueInXml(WSConstants.KEY_ERROR_MSG, xmlResponse);
				if (errorMSG != null) {
					errorMSG = WSConstants.WS_ERRORMESSAGE_PREFIX + " error - " + errorMSG;
					throw new WebserviceClientException(errorMSG);
				} else {
					throw new WebserviceClientException(xmlResponse);
				}

			}
		}

		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return result;
	}

	/**
	 * Delete  location using primary key aka surrogate id of  location
	 * @param params
	 * @return
	 * @throws WebserviceClientException
	 */
	public static String deleteLocation(String surrogateId) throws WebserviceClientException {
		String result = null;
		String self = "WebServiceClientImpl.deleteLocation(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		String xmlResponse = null;
		try {
			xmlResponse = new WebServiceInvoker().performWSCall(WSConstants.METHOD_DELETE,
					WSConstants.URI_SINGLE_LOCATION, HostIP, null, null, surrogateId);
		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage(), e);
			String errorMSG = null;
			if (e.getMessage() != null){
				errorMSG = findKeyValueInXml(WSConstants.KEY_ERROR_MSG,  e.getMessage().substring(e.getMessage().indexOf("Error:")+6).trim());
			}
			if (errorMSG != null) {
				errorMSG = WSConstants.WS_ERRORMESSAGE_PREFIX + " error - " + errorMSG;
				throw new WebserviceClientException(errorMSG);
			} else {
				throw new WebserviceClientException(e);
			}		
		}

		if (xmlResponse != null && !xmlResponse.isEmpty()) {
			CommonLogger.error(LoggerTypes.DEBUG, self + "xmlResponse:"+xmlResponse);
			if (xmlResponse.contains("error")) {
				String errorMSG = WSConstants.WS_ERRORMESSAGE_PREFIX + " error - "
						+ findKeyValueInXml(WSConstants.KEY_ERROR_MSG, xmlResponse);
				if (errorMSG != null) {
					throw new WebserviceClientException(errorMSG);
				}
			}
		}
		result = "success";
		CommonLogger.error(LoggerTypes.DEBUG, self + "xmlResponse : " + xmlResponse);
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return result;
	}

	/**
	 * Update this method in future if other fields came 
	 * Create xml from provided parameters of location
	 * @param loc_name
	 * @param correlationId
	 * @param locationPatterns
	 * @return
	 */
	public static String createLocationXml(String loc_name, String correlationId, List<String> locationPatterns) {
		String xmlPayload = null;
		String self = "WebServiceClientImpl.createLocationXml(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		try {
			Location locationData = new Location();
			locationData.setName(loc_name);
			locationData.setCorrelation(correlationId);//unique value for authorized location

			List<LocationPattern> locPatternList = new ArrayList<LocationPattern>();
			if (locationPatterns != null) {
				for (String ipaddrPattern : locationPatterns) {
					LocationPattern locpattern = new LocationPattern();
					locpattern.setIpaddresspattern(ipaddrPattern);
					locPatternList.add(locpattern);
				}
			}
			locationData.setLocationPatterns(locPatternList);
			xmlPayload = createLocationXml(locationData);

		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage());
		}

		CommonLogger.info(LoggerTypes.DEBUG, self + "Result\n" + xmlPayload);
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return xmlPayload;
	}

	/**
	 * Create xml from provided location data
	 * @param locationdata
	 * @return
	 */
	public static String createLocationXml(Location locationData) {
		String xmlPayload = null;
		String self = "WebServiceClientImpl.createLocationXml(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		try {
			JAXBContext context = JAXBContext.newInstance(Location.class);
			JAXBElement<Location> je2 = new JAXBElement<Location>(new QName("location"), Location.class, locationData);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter strW = new StringWriter();
			marshaller.marshal(je2, strW);
			xmlPayload = strW.toString();

		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage());
		}

		CommonLogger.info(LoggerTypes.DEBUG, self + "Result\n" + xmlPayload);
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return xmlPayload;
	}

	public static Location extractLocationFromXml(String xmlPayload) {

		String self = "WebServiceClientImpl.extractLocationFromXml(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		Location locationData = null;
		if (xmlPayload != null) {

			try {
				JAXBContext context = JAXBContext.newInstance(Location.class);
				Unmarshaller unMarshaller = context.createUnmarshaller();
				StringReader strR = new StringReader(xmlPayload);
				locationData = (Location) unMarshaller.unmarshal(strR);

			} catch (Exception e) {
				CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage());
			}
		}

		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return locationData;
	}

	/**
	 * For getting key from Xml
	 * @param key
	 * @param xmlPayload
	 * @return
	 */
	public static String findKeyValueInXml(String key, String xmlPayload) {
		String self = "WebServiceClientImpl.findKeyValueInXml(): ";
		CommonLogger.finest(LoggerTypes.DEBUG, self + "Enter");

		String value = null;
		try {
			if (key != null && xmlPayload != null) {
				Document doc = null;

				InputStream is = new ByteArrayInputStream(xmlPayload.getBytes("UTF-8"));
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setIgnoringComments(true);
				DocumentBuilder db = dbf.newDocumentBuilder();
				db.setEntityResolver(new EntityResolver() {
					public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
						return new InputSource(new StringReader(""));
					}
				});

				doc = db.parse(is);

				XPath xPath = XPathFactory.newInstance().newXPath();

				if (key.equals(WSConstants.KEY_ERROR_MSG)) {

					XPathExpression xPathExpression = xPath.compile(WSConstants.XPATH_ERROR_MSG);
					value = (String) xPathExpression.evaluate(doc);
					CommonLogger.info(LoggerTypes.DEBUG, "errorMsg: " + value);
				} else if (key.equals(WSConstants.KEY_LOCATION_NAME)) {

					XPathExpression xPathExpression = xPath.compile(WSConstants.XPATH_LOCATION_NAME);
					value = (String) xPathExpression.evaluate(doc);
					CommonLogger.info(LoggerTypes.DEBUG, "Name: " + value);

				} else if (key.equals(WSConstants.KEY_CORRELATION)) {

					XPathExpression xPathExpression = xPath.compile(WSConstants.XPATH_CORRELATION);
					value = (String) xPathExpression.evaluate(doc);
					CommonLogger.info(LoggerTypes.DEBUG, "correl: " + value);

				} else if (key.equals(WSConstants.KEY_SURROGATE_LOCATION_ID)) {

					XPathExpression xPathExpression = xPath.compile(WSConstants.XPATH_SURROGATE_LOCATION_ID);
					String hrefLink = (String) xPathExpression.evaluate(doc);
					CommonLogger.info(LoggerTypes.DEBUG, "link: " + hrefLink);
					value = getSurrogateIdFromLink(hrefLink);

					CommonLogger.info(LoggerTypes.DEBUG, "Surrogate key: : " + value);

				}
			}
		} catch (Exception e) {
			CommonLogger.error(LoggerTypes.DEBUG, self + e.getMessage());
		}

		CommonLogger.finest(LoggerTypes.DEBUG, self + "Exit");

		return value;
	}

	private static String getSurrogateIdFromLink(String hrefLink) {
		String value = null;
		String[] tokens = hrefLink.split("/");

		//read index of "locations" from tag <link href="https://148.147.178.109/NRP/admin/locations/98307" rel="self"/>
		for (int i = tokens.length; i > 0; i--) {
			if (tokens[i - 1].equals(WSConstants.LOCATIONS)) {
				value = tokens[i];
				break;
			}
		}

		return value;
	}

	public static void main(String argv[]) {
		/*List<String> list = new ArrayList<String>();
		list.add("48.47.29.1-48.47.29.4");
		list.add("48.47.29.1-48.47.29.4");
		String xml = createLocationXml("test1", "12345", list);
		System.out.println("XML:" + xml);
		Location loc = extractLocationFromXml(xml);
		System.out.println("Location from XML Name:" + loc.getName());
		System.out.println("Location from XML Correlation:" + loc.getCorrelation());
		System.out.println("Location from XML Pattern :" + loc.getLocationPatterns().get(0));*/

		String xml = "<location><name>1385111857313</name><cac_audio_alarm_latency>5</cac_audio_alarm_latency><cac_audio_alarm_threshold_percent>80</cac_audio_alarm_threshold_percent><AverageBandwidthPerCall>80</AverageBandwidthPerCall><AverageBandwidthPerCallUnitOfMeasurement>Kbit/sec</AverageBandwidthPerCallUnitOfMeasurement><cac_can_audio_steal_from_video>true</cac_can_audio_steal_from_video><cac_max_bwidth_video_interloc>2000</cac_max_bwidth_video_interloc><cac_max_bwidth_video_intraloc>2000</cac_max_bwidth_video_intraloc><cac_min_acceptable_bwidth_video>64</cac_min_acceptable_bwidth_video><ManagedBandwidthUnitOfMeasurement>Kbit/sec</ManagedBandwidthUnitOfMeasurement><cac_video_alarm_latency>5</cac_video_alarm_latency><cac_video_alarm_threshold_percent>80</cac_video_alarm_threshold_percent><correlation>1385111857313</correlation><dpt_in_survivable_mode>false</dpt_in_survivable_mode><TimeToLiveInSec>3600</TimeToLiveInSec>"
				+ "<link href=\"https://hostname/NRP/admin/locations/98307\" rel=\"self\"/>"
				+ "<locationpattern><ipaddresspattern>48.47.29.1-48.47.29.4</ipaddresspattern><link href=\"https://hostname/NRP/admin/locations/98307/locationpatterns/32771\" rel=\"self\"/></locationpattern></location>";
		/*String xml ="<error><status></status><errorMsg>123123</errorMsg></error>";
				*/
		System.out.println("Location :" + findKeyValueInXml(WSConstants.KEY_SURROGATE_LOCATION_ID, xml));

	}

}
