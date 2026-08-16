package med.voll.web_application.domain.consulta;

import jakarta.transaction.Transactional;
import med.voll.web_application.domain.medico.MedicoRepository;
import med.voll.web_application.infra.telemetry.StructuredLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private static final Logger logger = LoggerFactory.getLogger(ConsultaService.class);
    private final ConsultaRepository repository;
    private final MedicoRepository medicoRepository;
    private final StructuredLogger structuredLogger;

    public ConsultaService(ConsultaRepository repository, MedicoRepository medicoRepository, StructuredLogger structuredLogger) {
        this.repository = repository;
        this.medicoRepository = medicoRepository;
        this.structuredLogger = structuredLogger;
    }

    public Page<DadosListagemConsulta> listar(Pageable paginacao) {
        logger.info("Listing appointments - Page: {}, Size: {}", paginacao.getPageNumber(), paginacao.getPageSize());
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "list-appointments",
            "page", String.valueOf(paginacao.getPageNumber()),
            "page_size", String.valueOf(paginacao.getPageSize()),
            "entity", "consulta"
        );
        structuredLogger.info("Appointments listing initiated", attributes);
        
        try {
            var resultado = repository.findAllByOrderByData(paginacao).map(DadosListagemConsulta::new);
            logger.debug("Total appointments found: {}", resultado.getTotalElements());
            attributes.put("total_elements", String.valueOf(resultado.getTotalElements()));
            structuredLogger.debug("Appointments list retrieved successfully", attributes);
            
            return resultado;
        } catch (Exception e) {
            logger.error("Error listing appointments", e);
            attributes.put("error_type", "query_error");
            structuredLogger.error("Failed to list appointments", e, attributes);
            throw e;
        }
    }

    @Transactional
    public void cadastrar(DadosAgendamentoConsulta dados) {
        logger.info("Starting appointment booking - Doctor ID: {}, Patient: {}, Date: {}", 
                   dados.idMedico(), dados.paciente(), dados.data());
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "book-appointment",
            "doctor_id", String.valueOf(dados.idMedico()),
            "patient", dados.paciente(),
            "appointment_date", dados.data().toString(),
            "entity", "consulta"
        );
        structuredLogger.info("Appointment booking initiated", attributes);
        
        try {
            var medicoConsulta = medicoRepository.findById(dados.idMedico()).orElseThrow(() -> {
                logger.error("Doctor not found - ID: {}", dados.idMedico());
                attributes.put("reason", "doctor_not_found");
                structuredLogger.error("Doctor not found during appointment booking", attributes);
                return new IllegalArgumentException("Doctor not found");
            });
            
            if (dados.id() == null) {
                repository.save(new Consulta(medicoConsulta, dados));
                logger.info("New appointment booked successfully - Doctor: {}, Patient: {}, Date: {}", 
                           medicoConsulta.getNome(), dados.paciente(), dados.data());
                attributes.put("action", "create");
                attributes.put("doctor_name", medicoConsulta.getNome());
                structuredLogger.info("Appointment booked successfully", attributes);
            } else {
                var consulta = repository.findById(dados.id()).orElseThrow(() -> {
                    logger.error("Appointment not found - ID: {}", dados.id());
                    attributes.put("reason", "appointment_not_found");
                    attributes.put("appointment_id", String.valueOf(dados.id()));
                    structuredLogger.error("Appointment not found during update", attributes);
                    return new IllegalArgumentException("Appointment not found");
                });
                consulta.modificarDados(medicoConsulta, dados);
                logger.info("Appointment updated - ID: {}, Doctor: {}, Date: {}", 
                           dados.id(), medicoConsulta.getNome(), dados.data());
                attributes.put("action", "update");
                attributes.put("doctor_name", medicoConsulta.getNome());
                attributes.put("appointment_id", String.valueOf(dados.id()));
                structuredLogger.info("Appointment updated successfully", attributes);
            }
        } catch (Exception e) {
            logger.error("Error booking appointment - Doctor ID: {}, Patient: {}", dados.idMedico(), dados.paciente(), e);
            attributes.put("error_type", "system_error");
            structuredLogger.error("Error during appointment booking", e, attributes);
            throw e;
        }
    }

    public DadosAgendamentoConsulta carregarPorId(Long id) {
        logger.debug("Loading appointment by ID: {}", id);
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "load-appointment",
            "appointment_id", String.valueOf(id),
            "entity", "consulta"
        );
        structuredLogger.debug("Loading appointment by ID", attributes);
        
        try {
            var consulta = repository.findById(id).orElseThrow(() -> {
                logger.error("Appointment not found - ID: {}", id);
                attributes.put("reason", "appointment_not_found");
                structuredLogger.error("Appointment not found during load", attributes);
                return new IllegalArgumentException("Appointment not found");
            });
            var medicoConsulta = medicoRepository.getReferenceById(consulta.getMedico().getId());
            logger.debug("Appointment loaded successfully - ID: {}, Doctor: {}", id, medicoConsulta.getNome());
            attributes.put("doctor_name", medicoConsulta.getNome());
            attributes.put("patient", consulta.getPaciente());
            structuredLogger.debug("Appointment loaded successfully", attributes);
            
            return new DadosAgendamentoConsulta(consulta.getId(), consulta.getMedico().getId(), 
                                               consulta.getPaciente(), consulta.getData(), 
                                               medicoConsulta.getEspecialidade());
        } catch (Exception e) {
            logger.error("Error loading appointment - ID: {}", id, e);
            attributes.put("error_type", "load_error");
            structuredLogger.error("Error loading appointment", e, attributes);
            throw e;
        }
    }

    @Transactional
    public void excluir(Long id) {
        logger.info("Deleting appointment - ID: {}", id);
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "delete-appointment",
            "appointment_id", String.valueOf(id),
            "entity", "consulta"
        );
        structuredLogger.info("Appointment deletion initiated", attributes);
        
        try {
            repository.deleteById(id);
            logger.info("Appointment deleted successfully - ID: {}", id);
            structuredLogger.info("Appointment deleted successfully", attributes);
        } catch (Exception e) {
            logger.error("Error deleting appointment - ID: {}", id, e);
            attributes.put("error_type", "delete_error");
            structuredLogger.error("Error deleting appointment", e, attributes);
            throw e;
        }
    }

}
