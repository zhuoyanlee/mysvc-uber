package alex.uber;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alex.uber.model.UberBooking;
import alex.uber.model.UberBookingResponse;
import alex.uber.service.func.UberService;
import alex.uber.service.security.AuthorisationService;

@RestController
public class UberController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    private AuthorisationService authService;
    
    @Autowired
    private UberService uberService;
    
    private final Logger logger = LoggerFactory.getLogger(UberController.class);
    
    @RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                            String.format(template, name));
    }
    
    @RequestMapping(value="/book", method=RequestMethod.POST)
    public UberBookingResponse init(@RequestBody UberBooking uberBooking) throws Exception{
    	
    	logger.info("Pickup address: {}", uberBooking.getPickupAddress());
    	logger.info("Dropoff address: {}", uberBooking.getDropoffAddress());
    	UberBookingResponse response = uberService.bookUberRide(uberBooking.getPickupAddress(), uberBooking.getDropoffAddress());
        
        
        
        return response;
    }
}
