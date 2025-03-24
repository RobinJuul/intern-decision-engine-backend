package ee.taltech.inbankbackend.service;

import ee.taltech.inbankbackend.exceptions.NoValidLoanException;

public interface AgeValidator {
    void verifyAgeEligibility(String personalCode) throws NoValidLoanException;
}
