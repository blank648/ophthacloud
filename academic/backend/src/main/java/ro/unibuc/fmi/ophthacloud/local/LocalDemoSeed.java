package ro.unibuc.fmi.ophthacloud.local;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
class LocalDemoSeed implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;

    LocalDemoSeed(JdbcTemplate jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var hash = passwords.encode("OphthaDemo!2026#19");
        var patientUser = UUID.fromString("60000000-0000-4000-8000-000000000019");
        account(patientUser, "pacient.demo@ophthacloud.ro", "PATIENT", hash);
        account(UUID.fromString("70000000-0000-4000-8000-000000000019"), "receptie.demo@ophthacloud.ro", "RECEPTIONIST", hash);
        account(UUID.fromString("30000000-0000-4000-8000-000000000019"), "dr.popescu@ophthacloud.ro", "DOCTOR", hash);
        jdbc.update("""
                INSERT INTO patients (id, user_id, cnp, last_name, first_name, birth_date, sex)
                VALUES ('20000000-0000-4000-8000-000000000019', ?, '1850515400018', 'Demo', 'Pacient', DATE '1985-05-15', 'M')
                ON CONFLICT (id) DO NOTHING
                """, patientUser);
    }

    private void account(UUID id, String email, String role, String hash) {
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, role) VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, id, email, hash, role);
    }
}
