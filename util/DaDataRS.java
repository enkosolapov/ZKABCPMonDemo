package basos.util;

import java.io.IOException;

/*import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;*/

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Получение данных по юрлицам из API Подсказок (https://dadata.ru/api/suggest/#request-party) через JAX-RS 2.0.
 * 
 */
/* https://confluence.hflabs.ru/pages/viewpage.action?pageId=466681947
* !! пример на Java (из другого раздела, потому не сразу нашёл): https://github.com/leon0399/dadata
*/

// TODO: прогнать по части субъектов (ИНН на определённую цифру)
public class DaDataRS {
	private static final Logger logger = LoggerFactory.getLogger(DaDataRS.class);

// FIXME: хранить в настройках; устанавливать в конструкторе (?)
	private static final String SUGGEST_API_KEY = "Token be456659ebc263828636ae120112b8b6eccbc8c2";
	private static final String SUGGEST_TARGET_URL = "https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/party"; // http://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/party
	
	/** Дернуть сервис по ИНН.
	 * @return Json-ответ или пустая строка ("") при неуспехе или пустом ответе.
	 */
	public static String getSuggestionByInn(String inn) {
    	/*Resteasy*/Client rc = null;
    	Response resp = null;
    	String val = null;
	    try {
/* Примеры JAX_RS 2.0 клиента: http://www.benchresources.net/resteasy-jax-rs-web-service-using-jaxb-json-example/
 * https://docs.oracle.com/javaee/7/tutorial/jaxrs-client002.htm#BABJCIJC
 */
/*
// Fake service http://jsonplaceholder.typicode.com
        ResteasyClient rc = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = rc.target("http://jsonplaceholder.typicode.com/posts");
    	String prm = "{\"title\": \"заголовОК\", \"body\": \"туловИще\", \"userId\": "+inn+"}";
    	Response resp = target.property("Content-Type", MediaType.APPLICATION_JSON)  // MediaType.APPLICATION_JSON // MediaType.APPLICATION_FORM_URLENCODED
    						  .property("Accept", MediaType.APPLICATION_JSON) // MediaType.APPLICATION_JSON
    						  .request() // FORM_CONTENT_TYPE_WITH_CHARSET // MediaType.APPLICATION_FORM_URLENCODED
    						  .post(Entity.entity(prm, MediaType.APPLICATION_JSON)); // MediaType.APPLICATION_JSON
    						  //.get();
*/    						  
			
// FIXME: уровень приложения; одна подготовка, много вызовов
	    	rc = /*new ResteasyClientBuilder()*/ClientBuilder.newClient();/*.newBuilder().sslContext(getSslContext())
	    												.hostnameVerifier(getHostnameVerifier())
	    												.build();*/
/* Token передавать так, а не через target.property(), иначе сервис отвечал 401
 * http://stackoverflow.com/questions/21763700/how-to-set-http-header-in-resteasy-3-0-client-framework-with-resteasyclientbuil
 * http://stackoverflow.com/questions/6929378/how-to-set-http-header-in-resteasy-client-framework
 * http://stackoverflow.com/questions/1885844/resteasy-client-framework-authentication-credentials
 */
			rc.register(new ClientRequestFilter() {
				@Override
				public void filter(ClientRequestContext requestContext) throws IOException {
					requestContext.getHeaders().add("Authorization", SUGGEST_API_KEY);
				}
			});
	    	/*Resteasy*/WebTarget target = rc.target(SUGGEST_TARGET_URL);
	    	//inn = "1234554321"; // 
	    	String prm = "{\"query\": \""+inn+"\"}";
	    	try {
		    	resp = target.property("Content-Type", MediaType.APPLICATION_JSON)
		    						  .property("Accept", MediaType.APPLICATION_JSON)
		    						  .request()
		    						  .post(Entity.entity(prm, MediaType.APPLICATION_JSON)); //Entity.json(prm)
	    	} catch (ProcessingException e) {
	    		logger.info("getSuggestionByInn.  Unable to invoke request (no connection ?) with param: '{}'", prm);
	    		return "";
	    	}
	    	
	    	int responseCode = resp.getStatus();
// ?? HOWTO оптимальнее ??
	    	//Json val = resp.readEntity(Json.class);
	        val = resp.readEntity(String.class); // Read output in string format
			
	        logger.debug("getSuggestionByInn.  param: '{}', response: '{}', responseCode: {}", prm, val, responseCode);
	        
			if (responseCode != 200 || "{\"suggestions\":[]}".equals(val) ) {
	        	val = "";
	        }
	        
			return val;
			
	 	} finally {
	 		if (resp != null) {
	        	resp.close();
	        }
	        if (rc != null) {
	        	rc.close();
	        }
	 	}	

	} // public static String getSuggestionByInn(String inn)
	
/* How to configure wildfly to use https with ClientBuilder in resteasy ? http://stackoverflow.com/questions/30660860/how-to-configure-wildfly-to-use-https-with-clientbuilder-in-resteasy
 * А здесь недостающая в пред. статье реализация SecureTrustManager: http://stackoverflow.com/questions/10415607/jersey-client-set-proxy
 * Но в моём случае всё работает и без этого.
 */ 
/*    
	public static HostnameVerifier getHostnameVerifier() {
	    return (String hostname, javax.net.ssl.SSLSession sslSession) -> true;
	}
	
	public static class SecureTrustManager implements X509TrustManager {
	
	    @Override
	    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
	            throws CertificateException {
	    }
	
	    @Override
	    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
	            throws CertificateException {
	    }
	
	    @Override
	    public X509Certificate[] getAcceptedIssuers() {
	        return new X509Certificate[0];
	    }
	
	    public boolean isClientTrusted(X509Certificate[] arg0) {
	        return true;
	    }
	
	    public boolean isServerTrusted(X509Certificate[] arg0) {
	        return true;
	    }
	
	} // public static class SecureTrustManager implements X509TrustManager
	
	
	public static SSLContext getSslContext() {
	    SSLContext sslContext = null;
	    try {
	        sslContext = SSLContext.getInstance("SSL");
	        sslContext.init(null, new TrustManager[]{new  SecureTrustManager()}, new SecureRandom());
	    }
	    catch (NoSuchAlgorithmException | KeyManagementException ex) {
	        logger.error("getSslContext.  ERROR OCCURS", ex);
	    }
	    return sslContext;
	} // public static SSLContext getSslContext()
*/	
	
/*	// Проблемы передачи utf-8 в JAX-RS: https://habrahabr.ru/post/140270/
	public static final String FORM_CONTENT_TYPE_WITH_CHARSET = "application/x-www-form-urlencoded; charset=utf-8";
	public static final String APPLICATION_JSON_WITH_CHARSET = "application/json; charset=utf-8";
*/	
	
} // public class DaDataRS