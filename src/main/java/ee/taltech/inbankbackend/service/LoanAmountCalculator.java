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
     * @param requestedPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     */
    public Decision findValidLoanAmount(int creditModifier, Long loanAmount, int requestedPeriod) {
        int currentLoanAmount = loanAmount.intValue();

        // Decrease loan amount step by step until it's valid.
        while (currentLoanAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            int period = requestedPeriod;

            // Try increasing the period if the requested period is not enough.
            while (period <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                double creditScore = creditScoreCalculator.calculateCreditScore(creditModifier, (long) currentLoanAmount, period);

                if (creditScore >= 0.1) {
                    return new Decision(currentLoanAmount, period, null);
                }
                period += 6;
            }
            currentLoanAmount -= 100;
        }
        return new Decision(0, 0, "No valid loan found after adjusting amount and period.");
    }

    /**
     * Finds the maximum loan amount the customer qualifies for, within the allowed period.
     * @param requestedLoanPeriod Requested loan period
     * @param creditModifier The customer's credit modifier
     * @return A Decision object containing the maximum approved loan amount and period, and an error message (if any)
     */
    public Decision findMaximumLoanAmount(int requestedLoanPeriod, int creditModifier) {
        Long maxLoanAmount = Long.valueOf(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT);

        for (int period = DecisionEngineConstants.MAXIMUM_LOAN_PERIOD; period >= requestedLoanPeriod ; period -= 6) {
            Long currentAmount = maxLoanAmount;

            while (currentAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
                double creditScore = creditScoreCalculator.calculateCreditScore(creditModifier, currentAmount, period);

                if (creditScore >= 0.1) {
                    return new Decision(currentAmount.intValue(), period, null);
                }
                currentAmount -= 100;
            }

        }
        return new Decision(0, 0, "No valid maximum loan found.");
    }

}
