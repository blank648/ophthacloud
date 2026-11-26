package ro.unibuc.fmi.ophthacloud.securityaudit.api;

public interface CurrentUserProvider {
    AuthenticatedUser currentUser();
}
