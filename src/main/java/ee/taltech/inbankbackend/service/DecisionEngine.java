package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }


        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }
        if (loanAmount == 0) {
            // If loanAmount is 0, find the maximum possible loan amount
            Decision maxLoanDecision = findMaximumLoanAmount(loanPeriod);

            if (maxLoanDecision.getLoanAmount() > 0) {
                return maxLoanDecision;
            } else {
                throw new NoValidLoanException(maxLoanDecision.getErrorMessage());
            }
        }

        double creditScore = getCreditScore(loanAmount, loanPeriod);

        if (creditScore >= 0.1){
            // Scenario 1: User qualifies for a larger loan, return the maximum amount they qualify for
            Decision maxLoanDecision = findMaximumLoanAmount(loanPeriod);

            //Extracting the maximum loan amount from the Decision object
            Integer maxLoanAmount = maxLoanDecision.getLoanAmount();
            if (maxLoanAmount != null && maxLoanAmount > loanAmount) {
                return new Decision(maxLoanAmount, loanPeriod, null);
            } else {
                return new Decision(loanAmount.intValue(), loanPeriod, null);
            }
        } else{
            // Scenario 2: Requested amount is too high, reduce amount or increase period
            Decision validLoan = findValidLoanAmount(loanAmount, loanPeriod);
            if (validLoan.getLoanAmount() > 0) {
                return validLoan;
            } else {
                throw new NoValidLoanException("No valid loan found!");
            }
        }


    }

    /**
     * Finds the valid loan amount and period. If loan amount is too high, it is lowered until it is valid and if period
     * is too low, it is highered until a valid amount and period are found.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     */
    private Decision findValidLoanAmount(Long loanAmount, int loanPeriod) {
        int currentLoanAmount = loanAmount.intValue();

        // Adjusting loan amount, if not valid, decreasing by 100.
        while (currentLoanAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            int testPeriod = loanPeriod;

            // Adjusting loan period, if not valid, increasing by 6 months.
            while (testPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                double creditScore = getCreditScore((long) currentLoanAmount, testPeriod);

                if (creditScore >= 0.1) {
                    return new Decision(currentLoanAmount, testPeriod, null);
                }
                testPeriod += 6;
            }
            currentLoanAmount -= 100;
        }
        return new Decision(0, 0, "No valid loan found after adjusting amount and period.");
    }

    /**
     * Calculates the maximum loan amount possible for current customer Credit score must be equal or greater than 0.1.
     * Otherwise, the maximum loan amount is decreased until it is of correct value.
     * @param loanPeriod     Requested loan period
     * @return maximum loan amount possible to be lent to the customer
     */
    private Decision findMaximumLoanAmount(int loanPeriod) {
        Long maxLoanAmount = Long.valueOf(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT);

        while (maxLoanAmount >=  DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            int testPeriod = loanPeriod;

            while (testPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                double creditScore = getCreditScore(maxLoanAmount, testPeriod);

                if (creditScore >= 0.1) {
                    return new Decision(maxLoanAmount.intValue(), testPeriod, null);
                }
                testPeriod += 6;
            }
            maxLoanAmount -= 100;
        }
        return new Decision(0, loanPeriod, "No valid loan found after adjusting amount and period.");
    }


    /**
     * Calculates customer's credit score using the proper algorithm.
     * Used algorithm: credit score = ((creditModifier / loanAmount) * loanPeriod) / 10
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return Customer's credit score.
     */
    private double getCreditScore(Long loanAmount, int loanPeriod) {
        return (((double) creditModifier / loanAmount) * loanPeriod) / 10;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last two digits of their ID code.
     * Debt - 00...74
     * Segment 1 - 75...84
     * Segment 2 - 85...94
     * Segment 3 - 95...99
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 2));

        if (segment < 75) {
            return 0;
        } else if (segment < 85 ) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 95 ) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, NoValidLoanException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }


        if ((DecisionEngineConstants.MINIMUM_LOAN_AMOUNT > loanAmount)
                || (loanAmount > DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if ((DecisionEngineConstants.MINIMUM_LOAN_PERIOD > loanPeriod)
                || (loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

        // Age checking
        int customerAge = getCustomerAge(personalCode);
        int maximumAllowedAge = getMaximumAllowedAge();

        // If customer is underage
        if (customerAge < 18) {
            throw new NoValidLoanException("AGE_RESTRICTION:UNDERAGE");
        }

        // If customer is overage
        if (customerAge > maximumAllowedAge) {
            throw new NoValidLoanException("AGE_RESTRICTION:OVERAGE");
        }

    }


    /**
     * Calculates a customer's age, given an input of a customer's personalCode.
     * First digit of personal code represents the century the customer is born in
     * Second to seventh digit is the persons date of birth in the format YYMMDD.
     * @param personalCode Customer's personal ID code
     * @return Customer's age in years
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     */
    private int getCustomerAge(String personalCode) throws InvalidPersonalCodeException {
        // Extract birth date from personal code
        String birthDateString = personalCode.substring(1, 7);

        int year = Integer.parseInt(birthDateString.substring(0, 2));
        int month = Integer.parseInt(birthDateString.substring(2, 4));
        int day = Integer.parseInt(birthDateString.substring(4, 6));

        // Determining the century based on the first digit of the ID code
        char firstDigit = personalCode.charAt(0);
        int century;
        if (firstDigit == '1' || firstDigit == '2') {
            century = 1800;
        } else if (firstDigit == '3' || firstDigit == '4') {
            century = 1900;
        } else if (firstDigit == '5' || firstDigit == '6') {
            century = 2000;
        } else {
            throw new InvalidPersonalCodeException("Invalid personal ID code format!");
        }

        int birthYear = century + year;
        LocalDate birthDate = LocalDate.of(birthYear, month, day);
        LocalDate currentDate = LocalDate.now();

        return Period.between(birthDate, currentDate).getYears();
    }

    /**
     * Calculates the maximum allowed age for a customer. Since the maximum loan period is 48 months (4 years) then the
     * maximum age can be 78 (our chosen life expectancy) - 4 years
     * @return Maximum allowed age for a customer
     */
    private int getMaximumAllowedAge() {
        return 78 - 4;
    }
}
