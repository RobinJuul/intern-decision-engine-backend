package ee.taltech.inbankbackend.service;

import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoanAmountCalculator {
    private final CreditScoreCalculator creditScoreCalculator;

    @Autowired
    public LoanAmountCalculator(CreditScoreCalculator creditScoreCalculator) {
        this.creditScoreCalculator = creditScoreCalculator;
    }

    /**
     * Finds the valid loan amount and period. If loan amount is too high, it is lowered until it is valid and if period
     * is too low, it is highered until a valid amount and period are found.
     * @param creditModifier Credit modifier of the customer
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     */
    public Decision findValidLoanAmount(int creditModifier, Long loanAmount, int loanPeriod) {
        int currentLoanAmount = loanAmount.intValue();

        // Adjusting loan amount, if not valid, decreasing by 100.
        while (currentLoanAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            int testPeriod = loanPeriod;

            // Adjusting loan period, if not valid, increasing by 6 months.
            while (testPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                double creditScore = creditScoreCalculator.calculateCreditScore(creditModifier, (long) currentLoanAmount, testPeriod);

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
     * Finds the maximum loan amount the customer qualifies for, within the allowed period.
     * @param loanPeriod Requested loan period
     * @param creditModifier The customer's credit modifier
     * @return A Decision object containing the maximum approved loan amount and period, and an error message (if any)
     */
    public Decision findMaximumLoanAmount(int loanPeriod, int creditModifier) {
        Long maxLoanAmount = Long.valueOf(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT);

        while (maxLoanAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            int testPeriod = loanPeriod;

            while (testPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                double creditScore = creditScoreCalculator.calculateCreditScore(creditModifier, maxLoanAmount, testPeriod);

                if (creditScore >= 0.1) {
                    return new Decision(maxLoanAmount.intValue(), testPeriod, null);
                }
                testPeriod += 6;
            }
            maxLoanAmount -= 100;
        }
        return new Decision(0, 0, "No valid maximum loan found.");
    }

}
