package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final PersonalCodeValidator personalCodeValidator;
    private final CreditScoreCalculator creditScoreCalculator;
    private final AgeValidator ageValidator;
    private final LoanAmountCalculator loanAmountCalculator;

    @Autowired
    public DecisionEngine(PersonalCodeValidator personalCodeValidator,
                          CreditScoreCalculator creditScoreCalculator,
                          AgeValidator ageValidator,
                          LoanAmountCalculator loanAmountCalculator) {
        this.personalCodeValidator = personalCodeValidator;
        this.creditScoreCalculator = creditScoreCalculator;
        this.ageValidator = ageValidator;
        this.loanAmountCalculator = loanAmountCalculator;
    }

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
        if (!personalCodeValidator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }

        ageValidator.verifyAgeEligibility(personalCode);


        if (!isLoanAmountValid(loanAmount) || !isLoanPeriodValid(loanPeriod)) {
            throw new InvalidLoanPeriodException("Invalid loan amount or period!");
        }

        //Calculate Credit Modifier
        int creditModifier = getCreditModifier(personalCode);

        //Calculate Credit Score
        double creditScore =  creditScoreCalculator.calculateCreditScore(creditModifier, loanAmount, loanPeriod);

        if (creditScore >= 0.1) {
            // Find Maximum Loan Amount
            Decision maxLoanDecision = loanAmountCalculator.findMaximumLoanAmount(creditModifier, loanPeriod);
            if (maxLoanDecision.getLoanAmount() != null && maxLoanDecision.getLoanAmount() > loanAmount) {
                return maxLoanDecision;
            }
            return new Decision(loanAmount.intValue(), loanPeriod, null);
        } else {
            Decision validLoanDecision = loanAmountCalculator.findValidLoanAmount(creditModifier, loanAmount, loanPeriod);
            if (validLoanDecision.getLoanAmount() != null && validLoanDecision.getLoanAmount() > 0) {
                return validLoanDecision;
            } else {
                throw new InvalidLoanAmountException("No valid loan found!");
            }
        }
    }

    private boolean isLoanPeriodValid(int loanPeriod) {
        return loanPeriod >= 12 && loanPeriod <= 48;
    }

    private boolean isLoanAmountValid(Long loanAmount) {
        return loanAmount >= 2000 && loanAmount <= 10000;
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
    public int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() -2));

        if (segment < 75) return 0;
        else if (segment <85) return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        else if (segment <95) return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }






}
