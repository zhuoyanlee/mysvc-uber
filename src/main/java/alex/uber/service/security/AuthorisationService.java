package alex.uber.service.security;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.uber.sdk.rides.auth.OAuth2Credentials;
import com.uber.sdk.rides.client.CredentialsSession;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.SessionConfiguration.Environment;

@Component
public class AuthorisationService {

	private SessionConfiguration sessionConfiguration;
	
	private LocalServerReceiver localServerReceiver;
	
	private CredentialsSession session;
	
	@Value("${user.name}")
	private String userName;
	
	@Value("${clientId}")
	private String clientId;
	
	@Value("${clientSecret}")
	private String clientSecret;

	@PostConstruct
	public void createSessionConfiguration() throws Exception {
        // Load the client ID and secret from {@code resources/secrets.properties}. Ideally, your
        // secrets would not be kept local. Instead, have your server accept the redirect and return
        // you the accessToken for a userId.
        

        
//        if (clientId.equals("ucodmpjifWGfa-RvePT5yU_zha8BmZ1S") || clientSecret.equals("3xtCNs2UynEOF24U1u7YwFgEXhDTi7vkeeSvL7uH")) {
//            throw new IllegalArgumentException(
//                    "Please enter your client ID and secret in the resources/secrets.properties file.");
//        }

        // Start a local server to listen for the OAuth2 redirect.
        localServerReceiver = new LocalServerReceiver.Builder().setPort(8181).build();
        String redirectUri = localServerReceiver.getRedirectUri();

        this.sessionConfiguration =  new SessionConfiguration.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .setEnvironment(Environment.SANDBOX)
                .build();
        
        Credential credential = authenticate(System.getProperty("user.name"), this.sessionConfiguration);
        
        this.session = new CredentialsSession(this.sessionConfiguration, credential);
        
        System.out.println("Connecting to " + sessionConfiguration.getEndpointHost());
        System.out.println("Scopes " + sessionConfiguration.getScopes().size());
        System.out.println("Access Token: " + session.getAuthenticator().getCredential().getAccessToken());


    }

	public  Credential authenticate(String userId, SessionConfiguration config) throws Exception {
        OAuth2Credentials oAuth2Credentials = createOAuth2Credentials(config);

        // First try to load an existing Credential. If that credential is null, authenticate the user.
        Credential credential = oAuth2Credentials.loadCredential(userId);
        if (credential == null || credential.getAccessToken() == null) {
            // Send user to authorize your application.
            System.out.printf("Add the following redirect URI to your developer.uber.com application: %s%n",
                    oAuth2Credentials.getRedirectUri());
            System.out.println("Press Enter when done.");

            System.in.read();

            // Generate an authorization URL.
            String authorizationUrl = oAuth2Credentials.getAuthorizationUrl();
            System.out.printf("In your browser, navigate to: %s%n", authorizationUrl);
            System.out.println("Waiting for authentication...");

            // Wait for the authorization code.
            String authorizationCode = localServerReceiver.waitForCode();
            System.out.println("Authentication received.");

            // Authenticate the user with the authorization code.
            credential = oAuth2Credentials.authenticate(authorizationCode, userId);
        }
        localServerReceiver.stop();

        return credential;
    }
	
	/**
     * Creates an {@link OAuth2Credentials} object that can be used by any of the servlets.
     */
    public static OAuth2Credentials createOAuth2Credentials(SessionConfiguration sessionConfiguration) throws Exception {
    	
        // Store the users OAuth2 credentials in their home directory.
        File credentialDirectory =
                new File(System.getProperty("user.home") + File.separator + ".uber_credentials");
        credentialDirectory.setReadable(true, true);
        credentialDirectory.setWritable(true, true);
        // If you'd like to store them in memory or in a DB, any DataStoreFactory can be used.
        AbstractDataStoreFactory dataStoreFactory = new FileDataStoreFactory(credentialDirectory);

        // Build an OAuth2Credentials object with your secrets.
        return new OAuth2Credentials.Builder()
                .setCredentialDataStoreFactory(dataStoreFactory)
                .setRedirectUri(sessionConfiguration.getRedirectUri())
                .setClientSecrets(sessionConfiguration.getClientId(), sessionConfiguration.getClientSecret())
                .build();
    }
	
	public SessionConfiguration getSessionConfiguration() {
		return sessionConfiguration;
	}

	public void setSessionConfiguration(SessionConfiguration sessionConfiguration) {
		this.sessionConfiguration = sessionConfiguration;
	}

	public LocalServerReceiver getLocalServerReceiver() {
		return localServerReceiver;
	}

	public void setLocalServerReceiver(LocalServerReceiver localServerReceiver) {
		this.localServerReceiver = localServerReceiver;
	}

	public CredentialsSession getSession() {
		return session;
	}

	public void setSession(CredentialsSession session) {
		this.session = session;
	}
	
	
}
