package com.webservice.client;

public class WSConstants {

	public static final String METHOD_GET = "GET";

	public static final String METHOD_POST = "POST";

	public static final String METHOD_PUT = "PUT";

	public static final String METHOD_DELETE = "DELETE";

	public static final String KEY_CORRELATION = "correlation";

	public static final String KEY_LOCATION_NAME = "name";

	public static final String KEY_SURROGATE_LOCATION_ID = "loc_id";

	public static final String KEY_LOCATION_PATTERN = "location_pattern";
	
	public static final String LOCATIONS = "locations";
	
	public static final String KEY_ERROR_MSG = "errorMsg";

	public static final String XPATH_ERROR_MSG = "//message";//TODO: Check with  guys if this is final - earlier it used to errorMsg - now its like <message>Location Name is not between 3 and 64 characters</message>

	//public static final String XPATH_STATUS_MSG = "//status";

	public static final String XPATH_CORRELATION = "//location/correlation";

	public static final String XPATH_LOCATION_NAME = "//location/name";

	public static final String XPATH_SURROGATE_LOCATION_ID = "//location/link/@href";

	public static final String XPATH_LOCATION_PATTERN = "//locationpattern/ipaddresspattern[1]/text()";

	public static final String URI_ALL_LOCATIONS = "https://<HOST>/NRP/admin/locations";

	public static final String URI_SINGLE_LOCATION = "https://<HOST>/NRP/admin/locations/$1";

	public static final String URI_SINGLE_LOCATION_BY_CORRELATION = "https://<HOST>/NRP/admin/locations/correlation/$1";

	public static final String URI_SINGLE_LOCATION_BY_NAME = "https://<HOST>/NRP/admin/locations/name/$1";

	//either this or we can use uri URI_SINGLE_LOCATION and add matrix parameters to it
	public static final String URI_EDIT_LOCATION_NAME_AND_CORREL = "https://<HOST>/NRP/admin/locations/$1;name=$2;correlation=$3";
	
	public static final String WS_ERRORMESSAGE_PREFIX = " Correlated Location";
	
	public static final String NULL_RESPONSE_ERRORMESSAGE = "Null response from webservice";
	
	public static final String WS_ERRORMESSAGE_UNKNOWN = "Unknown error while communicating with  Correlated Location" ;

}
