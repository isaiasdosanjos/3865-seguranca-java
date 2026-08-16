package med.voll.web_application.domain.consulta;

import jakarta.transaction.Transactional;
import med.voll.web_application.domain.medico.MedicoRepository;
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

    public ConsultaService(ConsultaRepository repository, MedicoRepository medicoRepository) {
        this.repository = repository;
        this.medicoRepository = medicoRepository;
    }

    public Page<DadosListagemConsulta> listar(Pageable paginacao) {
        logger.info("Listing appointments - Page: {}, Size: {}", paginacao.getPageNumber(), paginacao.getPageSize());
        try {
            var resultado = repository.findAllByOrderByData(paginacao).map(DadosListagemConsulta::new);
            logger.debug("Total appointments found: {}", resultado.getTotalElements());
            return resultado;
        } catch (Exception e) {
            logger.error("Error listing appointments", e);
            throw e;
        }
    }

    @Transactional
    public void cadastrar(DadosAgendamentoConsulta dados) {
        logger.info("Starting appointment booking - Doctor ID: {}, Patient: {}, Date: {}", 
                   dados.idMedico(), dados.paciente(), dados.data());
        
        try {
            var medicoConsulta = medicoRepository.findById(dados.idMedico()).orElseThrow(() -> {
                logger.error("Doctor not found - ID: {}", dados.idMedico());
                return new IllegalArgumentException("Doctor not found");
            });
            
            if (dados.id() == null) {
                repository.save(new Consulta(medicoConsulta, dados));
                logger.info("New appointment booked successfully - Doctor: {}, Patient: {}, Date: {}", 
                           medicoConsulta.getNome(), dados.paciente(), dados.data());
            } else {
                var consulta = repository.findById(dados.id()).orElseThrow(() -> {
                    logger.error("Appointment not found - ID: {}", dados.id());
                    return new IllegalArgumentException("Appointment not found");
                });
                consulta.modificarDados(medicoConsulta, dados);
                logger.info("Appointment updated - ID: {}, Doctor: {}, Date: {}", 
                           dados.id(), medicoConsulta.getNome(), dados.data());
            }
        } catch (Exception e) {
            logger.error("Error booking appointment - Doctor ID: {}, Patient: {}", dados.idMedico(), dados.paciente(), e);
            throw e;
        }
    }

    public DadosAgendamentoConsulta carregarPorId(Long id) {
        logger.debug("Loading appointment by ID: {}", id);
        try {
            var consulta = repository.findById(id).orElseThrow(() -> {
                logger.error("Appointment not found - ID: {}", id);
                return new IllegalArgumentException("Appointment not found");
            });
            var medicoConsulta = medicoRepository.getReferenceById(consulta.getMedico().getId());
            logger.debug("Appointment loaded successfully - ID: {}, Doctor: {}", id, medicoConsulta.getNome());
            return new DadosAgendamentoConsulta(consulta.getId(), consulta.getMedico().getId(), 
                                               consulta.getPaciente(), consulta.getData(), 
                                               medicoConsulta.getEspecialidade());
        } catch (Exception e) {
            logger.error("Error loading appointment - ID: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public void excluir(Long id) {
        logger.info("Deleting appointment - ID: {}", id);
        try {
            repository.deleteById(id);
            logger.info("Appointment deleted successfully - ID: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting appointment - ID: {}", id, e);
            throw e;
        }
    }

}
