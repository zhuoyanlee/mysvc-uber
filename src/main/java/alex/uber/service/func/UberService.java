package alex.uber.service.func;

import java.io.IOException;
import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.error.ApiError;
import com.uber.sdk.rides.client.error.ClientError;
import com.uber.sdk.rides.client.error.ErrorParser;
import com.uber.sdk.rides.client.model.Location;
import com.uber.sdk.rides.client.model.Ride;
import com.uber.sdk.rides.client.model.RideEstimate;
import com.uber.sdk.rides.client.model.RideRequestParameters;
import com.uber.sdk.rides.client.model.UserProfile;
import com.uber.sdk.rides.client.services.RidesService;

import alex.uber.model.UberBookingResponse;
import alex.uber.service.security.AuthorisationService;
import retrofit2.Response;


@Service
public class UberService {

	private UberRidesApi uberRidesApi;
	
	private RidesService rideService;
	
	@Autowired
	private AuthorisationService authService;
	
	private final Logger logger = LoggerFactory.getLogger(UberService.class);
	
	@PostConstruct
	public void init() {
		
		this.uberRidesApi = UberRidesApi.with(authService.getSession()).build();
		
		this.rideService = uberRidesApi.createService();
		
	}
	public UberBookingResponse bookUberRide(final String pickupAddress, final String dropoffAddress) throws JsonProcessingException, IOException {
		
		UberBookingResponse uberResponse = new UberBookingResponse();
		
    	RideRequestParameters.Builder builder = new RideRequestParameters.Builder();
    	Location dropoff = getGeocodedLocation(dropoffAddress);
    	Location pickup = getGeocodedLocation(pickupAddress);
    	
    	builder.setDropoffCoordinates(dropoff.getLatitude(), dropoff.getLongitude());
    	builder.setPickupCoordinates(pickup.getLatitude(), pickup.getLongitude());
//    	builder.setPi
//    	builder.setPickupCoordinates(-35.362751f, 149.0967837f);
//    	builder.setDropoffCoordinates(-35.3587781f, 149.0847456f);
    	builder.setSeatCount(2);
    	
//    	RideRequestParameters param = new RideRequestParameters.Builder();
    	try {
    		Response<RideEstimate> response = this.rideService.estimateRide(builder.build()).execute();
    		
    		RideEstimate estimate = response.body();
    		uberResponse.setPriceEstimate(estimate.getEstimate().getLowEstimate() + " - " + estimate.getEstimate().getHighEstimate());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	try {
			Response<Ride> requestRide = this.rideService.requestRide(builder.build()).execute();
			uberResponse.setStatus(requestRide.message());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return uberResponse;
    }
	
	public UserProfile getUserProfile() throws IOException {
		// Fetch the user's profile.
        System.out.println("Calling API to get the user's profile");
        Response<UserProfile> response = rideService.getUserProfile().execute();

        ApiError apiError = ErrorParser.parseError(response);
        if (apiError != null) {
            // Handle error.
            ClientError clientError = apiError.getClientErrors().get(0);
            System.out.printf("Unable to fetch profile. %s", clientError.getTitle());
            System.exit(0);
            return null;
        }

        // Success!
        UserProfile userProfile = response.body();
        System.out.printf("Logged in as %s%n", userProfile.getEmail());
        System.out.printf("Last name is %s%n", userProfile.getLastName());
        
        return userProfile;
	}
	
	public Location getGeocodedLocation(String address) throws JsonProcessingException, IOException {
		RestTemplate restTemplate = new RestTemplate();
		
		address = address.replaceAll(" ", "+");
        String location = restTemplate.getForObject("https://maps.googleapis.com/maps/api/geocode/json?address=" + address + "&key=AIzaSyA8HH4RCU4-ZKTHG-7aQ7Uv86o9E0wQ3KA", String.class);
        
        ObjectMapper mapper =new ObjectMapper();
        JsonNode node = mapper.readTree(location);
        
        Iterator<JsonNode> results = node.path("results").elements();
        
        JsonNode result = results.next();
        String latitude = result.path("geometry").path("location").get("lat").asText();
        logger.info("Latitude: {} ", latitude);
        
        String longitude = result.path("geometry").path("location").get("lng").asText();
        logger.info("Longitude: {} ", longitude);
        return new Location(Float.parseFloat(latitude), Float.parseFloat(longitude));
		
	}
	public UberRidesApi getUberRidesApi() {
		return uberRidesApi;
	}
	public void setUberRidesApi(UberRidesApi uberRidesApi) {
		this.uberRidesApi = uberRidesApi;
	}
	public RidesService getRideService() {
		return rideService;
	}
	public void setRideService(RidesService rideService) {
		this.rideService = rideService;
	}
	public AuthorisationService getAuthService() {
		return authService;
	}
	public void setAuthService(AuthorisationService authService) {
		this.authService = authService;
	}
	
	
}
