package ee.taltech.inbankbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ee.taltech.inbankbackend" )
public class InbankBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InbankBackendApplication.class, args);
    }

}
