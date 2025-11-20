package service;

import com.pm.patientservice.dto.PatientResponseDTO;
import org.springframework.stereotype.Service;
import repository.PatientRepository;
import com.pm.patientservice.model.Patient;
import java.util.List;

@Service
public class PatientService {
    private PatientRepository patientRepository;
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients (){
        List<Patient> patients = patientRepository.findAll();
    }
}
