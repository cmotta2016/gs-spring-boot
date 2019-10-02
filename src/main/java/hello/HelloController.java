package hello;

import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController {

    @RequestMapping("/")
    public String index() throws SecurityException, IOException {
    	Logger logger = Logger.getLogger("HelloController");  
    	
    	FileHandler fh;  
    	// This block configure the logger with handler and formatter  
        fh = new FileHandler("/logs/console.log");  
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);  

        // the following statement is used to log any messages  
        logger.info("Greetings from Spring Boot!");  
        fh.close();
    	
        return "Greetings from Spring Boot!";
    }

}
