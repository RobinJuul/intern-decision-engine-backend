package ee.taltech.inbankbackend.service;

import org.springframework.stereotype.Service;

@Service
public class RegularCreditScoreCalculator implements CreditScoreCalculator {



    /**
     * Calculates customer's credit score using the proper algorithm.
     * Used algorithm: credit score = ((creditModifier / loanAmount) * loanPeriod) / 10
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @param creditModifier Credit modifier of customer
     * @return Customer's credit score.
     */
    @Override
    public double calculateCreditScore(int creditModifier, Long loanAmount, int loanPeriod) {
        return (((double) creditModifier / loanAmount) * loanPeriod) /10;
    }
}
