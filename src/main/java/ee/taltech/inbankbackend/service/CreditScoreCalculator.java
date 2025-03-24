package ee.taltech.inbankbackend.service;

public interface CreditScoreCalculator {
    double calculateCreditScore(int creditModifier, Long loanAmount, int loanPeriod);
}
