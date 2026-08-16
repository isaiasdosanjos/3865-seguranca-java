package med.voll.web_application.domain.medico;

import jakarta.transaction.Transactional;
import med.voll.web_application.domain.RegraDeNegocioException;
import med.voll.web_application.infra.telemetry.StructuredLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MedicoService {

    private static final Logger logger = LoggerFactory.getLogger(MedicoService.class);
    private final MedicoRepository repository;
    private final StructuredLogger structuredLogger;

    public MedicoService(MedicoRepository repository, StructuredLogger structuredLogger) {
        this.repository = repository;
        this.structuredLogger = structuredLogger;
    }

    public Page<DadosListagemMedico> listar(Pageable paginacao) {
        logger.info("Listing doctors - Page: {}, Size: {}", paginacao.getPageNumber(), paginacao.getPageSize());
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "list-doctors",
            "page", String.valueOf(paginacao.getPageNumber()),
            "page_size", String.valueOf(paginacao.getPageSize()),
            "entity", "medico"
        );
        structuredLogger.info("Doctors listing initiated", attributes);
        
        try {
            var resultado = repository.findAll(paginacao).map(DadosListagemMedico::new);
            logger.debug("Total doctors found: {}", resultado.getTotalElements());
            
            attributes.put("total_elements", String.valueOf(resultado.getTotalElements()));
            structuredLogger.debug("Doctors list retrieved successfully", attributes);
            
            return resultado;
        } catch (Exception e) {
            logger.error("Error listing doctors", e);
            attributes.put("error", e.getMessage());
            structuredLogger.error("Failed to list doctors", e, attributes);
            throw e;
        }
    }

    @Transactional
    public void cadastrar(DadosCadastroMedico dados) {
        logger.info("Starting doctor registration - Email: {}, CRM: {}", dados.email(), dados.crm());
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "register-doctor",
            "email", dados.email(),
            "crm", dados.crm(),
            "entity", "medico"
        );
        structuredLogger.info("Doctor registration initiated", attributes);
        
        if (repository.isJaCadastrado(dados.email(), dados.crm(), dados.id())) {
            logger.warn("Duplicate registration attempt - Email: {}, CRM: {}", dados.email(), dados.crm());
            attributes.put("reason", "duplicate_email_or_crm");
            structuredLogger.warn("Duplicate doctor registration attempt", attributes);
            throw new RegraDeNegocioException("E-mail ou CRM já cadastrado para outro médico!");
        }

        try {
            if (dados.id() == null) {
                repository.save(new Medico(dados));
                logger.info("New doctor registered successfully - Email: {}, CRM: {}", dados.email(), dados.crm());
                attributes.put("action", "create");
                structuredLogger.info("Doctor registered successfully", attributes);
            } else {
                var medico = repository.findById(dados.id()).orElseThrow(() -> {
                    logger.error("Doctor not found - ID: {}", dados.id());
                    attributes.put("doctor_id", String.valueOf(dados.id()));
                    attributes.put("reason", "doctor_not_found");
                    structuredLogger.error("Doctor not found during update", attributes);
                    return new IllegalArgumentException("Doctor not found");
                });
                medico.atualizarDados(dados);
                logger.info("Doctor updated - ID: {}, Email: {}", dados.id(), dados.email());
                attributes.put("action", "update");
                attributes.put("doctor_id", String.valueOf(dados.id()));
                structuredLogger.info("Doctor updated successfully", attributes);
            }
        } catch (RegraDeNegocioException e) {
            logger.warn("Business rule violation during registration: {}", e.getMessage());
            attributes.put("error_type", "business_rule");
            structuredLogger.error("Business rule violation in registration", e, attributes);
            throw e;
        } catch (Exception e) {
            logger.error("Error during doctor registration - Email: {}, CRM: {}", dados.email(), dados.crm(), e);
            attributes.put("error_type", "system_error");
            structuredLogger.error("Error during doctor registration", e, attributes);
            throw e;
        }
    }

    public DadosCadastroMedico carregarPorId(Long id) {
        logger.debug("Loading doctor by ID: {}", id);
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "load-doctor",
            "doctor_id", String.valueOf(id),
            "entity", "medico"
        );
        structuredLogger.debug("Loading doctor by ID", attributes);
        
        try {
            var medico = repository.findById(id).orElseThrow(() -> {
                logger.error("Doctor not found - ID: {}", id);
                attributes.put("reason", "doctor_not_found");
                structuredLogger.error("Doctor not found during load", attributes);
                return new IllegalArgumentException("Doctor not found");
            });
            logger.debug("Doctor loaded successfully - ID: {}, Email: {}", id, medico.getEmail());
            attributes.put("email", medico.getEmail());
            structuredLogger.debug("Doctor loaded successfully", attributes);
            
            return new DadosCadastroMedico(medico.getId(), medico.getNome(), medico.getEmail(), medico.getTelefone(), medico.getCrm(), medico.getEspecialidade());
        } catch (Exception e) {
            logger.error("Error loading doctor - ID: {}", id, e);
            attributes.put("error_type", "load_error");
            structuredLogger.error("Error loading doctor", e, attributes);
            throw e;
        }
    }

    @Transactional
    public void excluir(Long id) {
        logger.info("Deleting doctor - ID: {}", id);
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "delete-doctor",
            "doctor_id", String.valueOf(id),
            "entity", "medico"
        );
        structuredLogger.info("Doctor deletion initiated", attributes);
        
        try {
            repository.deleteById(id);
            logger.info("Doctor deleted successfully - ID: {}", id);
            structuredLogger.info("Doctor deleted successfully", attributes);
        } catch (Exception e) {
            logger.error("Error deleting doctor - ID: {}", id, e);
            attributes.put("error_type", "delete_error");
            structuredLogger.error("Error deleting doctor", e, attributes);
            throw e;
        }
    }

    public List<DadosListagemMedico> listarPorEspecialidade(Especialidade especialidade) {
        logger.info("Listing doctors by specialty: {}", especialidade);
        
        var attributes = StructuredLogger.createAttributes(
            "operation", "list-doctors-by-specialty",
            "specialty", especialidade.toString(),
            "entity", "medico"
        );
        structuredLogger.info("Doctors listing by specialty initiated", attributes);
        
        try {
            var resultado = repository.findByEspecialidade(especialidade).stream().map(DadosListagemMedico::new).toList();
            logger.debug("Found {} doctors with specialty: {}", resultado.size(), especialidade);
            attributes.put("total_found", String.valueOf(resultado.size()));
            structuredLogger.debug("Doctors retrieved by specialty", attributes);
            
            return resultado;
        } catch (Exception e) {
            logger.error("Error listing doctors by specialty: {}", especialidade, e);
            attributes.put("error_type", "query_error");
            structuredLogger.error("Error listing doctors by specialty", e, attributes);
            throw e;
        }
    }

}
