package med.voll.web_application.domain.medico;

import jakarta.transaction.Transactional;
import med.voll.web_application.domain.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoService {

    private static final Logger logger = LoggerFactory.getLogger(MedicoService.class);
    private final MedicoRepository repository;

    public MedicoService(MedicoRepository repository) {
        this.repository = repository;
    }

    public Page<DadosListagemMedico> listar(Pageable paginacao) {
        logger.info("Listing doctors - Page: {}, Size: {}", paginacao.getPageNumber(), paginacao.getPageSize());
        try {
            var resultado = repository.findAll(paginacao).map(DadosListagemMedico::new);
            logger.debug("Total doctors found: {}", resultado.getTotalElements());
            return resultado;
        } catch (Exception e) {
            logger.error("Error listing doctors", e);
            throw e;
        }
    }

    @Transactional
    public void cadastrar(DadosCadastroMedico dados) {
        logger.info("Starting doctor registration - Email: {}, CRM: {}", dados.email(), dados.crm());
        
        if (repository.isJaCadastrado(dados.email(), dados.crm(), dados.id())) {
            logger.warn("Duplicate registration attempt - Email: {}, CRM: {}", dados.email(), dados.crm());
            throw new RegraDeNegocioException("E-mail ou CRM já cadastrado para outro médico!");
        }

        try {
            if (dados.id() == null) {
                repository.save(new Medico(dados));
                logger.info("New doctor registered successfully - Email: {}, CRM: {}", dados.email(), dados.crm());
            } else {
                var medico = repository.findById(dados.id()).orElseThrow(() -> {
                    logger.error("Doctor not found - ID: {}", dados.id());
                    return new IllegalArgumentException("Doctor not found");
                });
                medico.atualizarDados(dados);
                logger.info("Doctor updated - ID: {}, Email: {}", dados.id(), dados.email());
            }
        } catch (RegraDeNegocioException e) {
            logger.warn("Business rule violation during registration: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error during doctor registration - Email: {}, CRM: {}", dados.email(), dados.crm(), e);
            throw e;
        }
    }

    public DadosCadastroMedico carregarPorId(Long id) {
        logger.debug("Loading doctor by ID: {}", id);
        try {
            var medico = repository.findById(id).orElseThrow(() -> {
                logger.error("Doctor not found - ID: {}", id);
                return new IllegalArgumentException("Doctor not found");
            });
            logger.debug("Doctor loaded successfully - ID: {}, Email: {}", id, medico.getEmail());
            return new DadosCadastroMedico(medico.getId(), medico.getNome(), medico.getEmail(), medico.getTelefone(), medico.getCrm(), medico.getEspecialidade());
        } catch (Exception e) {
            logger.error("Error loading doctor - ID: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public void excluir(Long id) {
        logger.info("Deleting doctor - ID: {}", id);
        try {
            repository.deleteById(id);
            logger.info("Doctor deleted successfully - ID: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting doctor - ID: {}", id, e);
            throw e;
        }
    }

    public List<DadosListagemMedico> listarPorEspecialidade(Especialidade especialidade) {
        logger.info("Listing doctors by specialty: {}", especialidade);
        try {
            var resultado = repository.findByEspecialidade(especialidade).stream().map(DadosListagemMedico::new).toList();
            logger.debug("Found {} doctors with specialty: {}", resultado.size(), especialidade);
            return resultado;
        } catch (Exception e) {
            logger.error("Error listing doctors by specialty: {}", especialidade, e);
            throw e;
        }
    }

}
