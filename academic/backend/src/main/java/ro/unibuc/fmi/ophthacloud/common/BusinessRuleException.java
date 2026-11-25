package ro.unibuc.fmi.ophthacloud.common;

public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String message) {
        this("BUSINESS_RULE_VIOLATION", message);
    }

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
