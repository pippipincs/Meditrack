package repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import com.pm.patientservice.model.Patient;
public interface PatientRepository extends JpaRepository<Patient, UUID> {

}
