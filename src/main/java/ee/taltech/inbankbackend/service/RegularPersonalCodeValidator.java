package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import org.springframework.stereotype.Service;

@Service
public class RegularPersonalCodeValidator implements PersonalCodeValidator {
    private final EstonianPersonalCodeValidator estonianPersonalCodeValidator = new EstonianPersonalCodeValidator();

    @Override
    public boolean isValid(String personalCode) throws InvalidPersonalCodeException {
        if (personalCode == null || personalCode.isEmpty() || !estonianPersonalCodeValidator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        return true;
    }
}
