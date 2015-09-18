package org.pxu.tools.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** 
 * Demonstrates how a httpservice can be exposed ad-hoc and tested.
 * The example below shows service interaction.
 * The test starts two services.
 * The first is a HTTP service which simply returns a data (FIRST_RESP_JSON) for a URL request.
 * The second is a HTTP service which is invoked via POST containing URL of the first service. When the POST request is
 * received, the second service will invoke the first HTTPS service and returns the response from the first service as
 * part of its response.
 * 
 * @author Prabhu Periasamy
 *
 */
public class HttpServiceUtilTest {
	private final String FIRST_RESP_JSON = "{\"firstServiceMessage\":\"Success: Your request was successfully completed.\",\"responseCode\":2000}";
	private final String SECOND_RESP_JSON = "{\"secondServiceMessage\": FIRST_RESP }";
	private final String FIRST_SERVICE_URL="/first_service/";
	private final String SECOND_SERVICE_URL = "/second_service/";
	
	@Test
	public void serviceCallBackExample() throws Exception {
		
		HttpServer httpServerOne=null;
		HttpServer httpServerTwo=null;
		try {
		 // Create a URL handler
		 HttpServiceUtil.RequestHandler firstReqHandler = new HttpServiceUtil.RequestHandler(FIRST_RESP_JSON, "text/json", null);
		 Map<String, HttpHandler> firstUrlHandlers = new HashMap<String, HttpHandler>();
		 firstUrlHandlers.put(FIRST_SERVICE_URL, firstReqHandler);
  
		 // Register url handlers to HTTP service
		 httpServerOne = HttpServiceUtil.getHttpService(firstUrlHandlers);
		 
		 // Display where it is running
		 int httpPortOne = httpServerOne.getAddress().getPort();
		 String hostName = httpServerOne.getAddress().getHostName();
		 String firstServiceUrl = "http://" + hostName + ":"+httpPortOne + FIRST_SERVICE_URL;
		 System.out.println("First Http service is running on URL: " + firstServiceUrl);	 
	 
		 // Create a custom handler for the second service
		 Map<String, HttpHandler> secondUrlHandlers = new HashMap<String, HttpHandler>();
		 HttpHandler customHandler = new HttpHandler() {

			public void handle(HttpExchange t) throws IOException {
				
				Map params = (Map) t.getAttribute("parameters");
				String firstUrl = (String) params.get("firstUrl");
				System.out.println("Second service got the first service url: " + firstUrl);
				// Invoking first first
				String firstResp = sendGet(firstUrl);
				String returnContent = SECOND_RESP_JSON.replace("FIRST_RESP", firstResp);
				t.getResponseHeaders().add("Content-type", MediaType.APPLICATION_JSON);
				t.sendResponseHeaders(200, returnContent.length());
				OutputStream os = t.getResponseBody();
				os.write(returnContent.getBytes());
				os.close();
			}
		 };
		 
		 secondUrlHandlers.put(SECOND_SERVICE_URL, customHandler);
		 
		 // Register the handler and start the service
		 httpServerTwo = HttpServiceUtil.getHttpService(secondUrlHandlers);
		 int httpPortTwo = httpServerTwo.getAddress().getPort();
		 
		 String secondServiceUrl = "http://" + hostName + ":"+httpPortTwo + SECOND_SERVICE_URL;
		 System.out.println("Second Http service is running on URL: " + secondServiceUrl);

		 System.out.println("Invoking second service...");
		 Map<String, String> urlParams = new HashMap<String, String>();
		 urlParams.put("firstUrl", firstServiceUrl);
		 String secondResp = sendPost(secondServiceUrl, "firstUrl=" + firstServiceUrl);
		 System.out.println("Response from second service: " + secondResp);
		 
//		 System.out.println("Response from first service...: " + sendGet(firstServiceUrl));
		 
		}
		finally {
			stop(httpServerOne);
			stop(httpServerTwo);
		}
		  
	}
	
	// HTTP GET request
	private String sendGet(String url) throws IOException {
		try {
		URL obj = new URL(url);

		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");
		
		System.out.println("\nSending 'GET' request to URL : " + url);
		
		int responseCode = con.getResponseCode();
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	// HTTP POST request
	private String sendPost(String url, String urlParams) throws Exception {

		byte[] postData       = urlParams.getBytes( StandardCharsets.UTF_8 );
		int    postDataLength = postData.length;
		
		URL obj = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
		conn.setDoOutput( true );
		conn.setInstanceFollowRedirects( false );
		conn.setRequestMethod( "POST" );
		conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
		conn.setRequestProperty( "charset", "utf-8");
		conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
		conn.setUseCaches( false );
		try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
		   wr.write( postData );
		}
		
        Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder returnStr = new StringBuilder();
        for ( int c = in.read(); c != -1; c = in.read() )
           returnStr.append((char)c);
		
		return returnStr.toString();
	}
	
    private void stop(HttpServer server) {
    	try {if (server==null) return; server.stop(0);} catch (Exception ignore) { }
    }
}
