package ee.taltech.inbankbackend.service;

import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Service
public class RegularAgeValidator implements AgeValidator {

    /**
     * Verifies if a customer's age is eligible for a loan. If a customer is under the age of 18, they are underage
     * If a customer is over the age of 78 - 4, they are overage and do not qualify for a loan.
     * @param personalCode Customer's personal ID code
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    @Override
    public void verifyAgeEligibility(String personalCode) throws NoValidLoanException {
        int age = getCustomerAge(personalCode);
        int maxAge = 78 - 4; // Maximum age to be eligible for a loan

        if (age < 18 )throw new NoValidLoanException("AGE_RESRTRICTION:UNDERAGE");
        if (age > maxAge) throw new NoValidLoanException("AGE_RESRTRICTION:OVERAGE");
    }

    /**
     * Calculates a customer's age, given an input of a customer's personalCode.
     * First digit of personal code represents the century the customer is born in
     * Second to seventh digit is the persons date of birth in the format YYMMDD.
     * @param personalCode Customer's personal ID code
     * @return Customer's age in years
     */
    private int getCustomerAge(String personalCode) {
        String birthDateString = personalCode.substring(1, 7);

        int year = Integer.parseInt(birthDateString.substring(0, 2));
        int month = Integer.parseInt(birthDateString.substring(2, 4));
        int day = Integer.parseInt(birthDateString.substring(4, 6));

        // Determining the century based on the first digit of the ID code
        char firstDigit = personalCode.charAt(0);
        int century = switch (personalCode.charAt(0)) {
            case '1', '2' -> 1800;
            case '3', '4' -> 1900;
            case '5', '6' -> 2000;
            default -> throw new IllegalArgumentException("Invalid personal code format");
        };


        LocalDate birthDate = LocalDate.of(century + year, month, day);

        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
