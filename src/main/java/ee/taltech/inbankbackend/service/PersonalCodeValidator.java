package ee.taltech.inbankbackend.service;

import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;

public interface PersonalCodeValidator {
    boolean isValid(String personalCode) throws InvalidPersonalCodeException;
}
