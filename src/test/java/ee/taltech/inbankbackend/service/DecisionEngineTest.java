package ee.taltech.inbankbackend.service;

import ee.taltech.inbankbackend.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

 class DecisionEngineTest {

    @Mock
    private PersonalCodeValidator personalCodeValidator;

    @Mock
    private CreditScoreCalculator creditScoreCalculator;

    @Mock
    private AgeValidator ageValidator;

    @Mock
    private LoanAmountCalculator loanAmountCalculator;

    @InjectMocks
    private DecisionEngine decisionEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initializes mocks and injects them into the DecisionEngine
    }

    @Test
    void testValidLoanCalculation() throws  InvalidLoanPeriodException, NoValidLoanException, InvalidPersonalCodeException, InvalidLoanAmountException {
        String personalCode = "49501234567";
        Long loanAmount = 3000L;
        int loanPeriod = 24;

        when(personalCodeValidator.isValid(personalCode)).thenReturn(true);
        doNothing().when(ageValidator).verifyAgeEligibility(personalCode);
        when(creditScoreCalculator.calculateCreditScore(anyInt(), eq(loanAmount), eq(loanPeriod)))
                .thenReturn(0.2);
        when(loanAmountCalculator.findMaximumLoanAmount(anyInt(), eq(loanPeriod)))
                .thenReturn(new Decision(3000, 24, null));

        Decision decision = decisionEngine.calculateApprovedLoan(personalCode, loanAmount, loanPeriod);

        assertNotNull(decision);
        assertEquals(3000, decision.getLoanAmount());
        assertEquals(24, decision.getLoanPeriod());
        assertNull(decision.getErrorMessage());
    }

    @Test
    void testInvalidPersonalCode() throws InvalidPersonalCodeException {
        String personalCode = "1234567890";
        Long loanAmount = 3000L;
        int loanPeriod = 24;

        when(personalCodeValidator.isValid(personalCode)).thenReturn(false);

        assertThrows(InvalidPersonalCodeException.class, () -> {
            decisionEngine.calculateApprovedLoan(personalCode, loanAmount, loanPeriod);
        });
    }

    @Test
    void testUnderageUser() throws  InvalidPersonalCodeException, NoValidLoanException {
        String personalCode = "49501234567";
        Long loanAmount = 3000L;
        int loanPeriod = 24;

        when(personalCodeValidator.isValid(personalCode)).thenReturn(true);
        doThrow(new NoValidLoanException("AGE_RESTRICTION:UNDERAGE")).when(ageValidator).verifyAgeEligibility(personalCode);

        assertThrows(NoValidLoanException.class, () -> {
            decisionEngine.calculateApprovedLoan(personalCode, loanAmount, loanPeriod);
        });
    }
}