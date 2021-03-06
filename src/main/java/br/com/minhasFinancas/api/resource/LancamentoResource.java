package br.com.minhasFinancas.api.resource;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.minhasFinancas.api.dto.AtualizaStatusDTO;
import br.com.minhasFinancas.api.dto.LancamentoDTO;
import br.com.minhasFinancas.exception.RegraNegocioException;
import br.com.minhasFinancas.model.entity.Lancamento;
import br.com.minhasFinancas.model.entity.Usuario;
import br.com.minhasFinancas.model.enums.StatusLancamento;
import br.com.minhasFinancas.model.enums.TipoLancamento;
import br.com.minhasFinancas.service.LancamentoService;
import br.com.minhasFinancas.service.UsuarioService;

@RestController
@RequestMapping("/api/lancamentos")
//@RequiredArgsConstructor // implementa 1 contrutor com os argumentos obrigatórios ou sejá os declarados como final
public class LancamentoResource {

	private LancamentoService service;
	private UsuarioService usuarioService;
	
	public LancamentoResource(LancamentoService service, UsuarioService usuarioService) {
		this.service = service;
		this.usuarioService = usuarioService;
	}
	
	@PutMapping("/{id}/atualiza-status") 
	public ResponseEntity atualizarStatus( @PathVariable("id") Long id, @RequestBody AtualizaStatusDTO dto ) {
		return service.obterPorId(id).map( entity -> {
			StatusLancamento statusSelecionado = StatusLancamento.valueOf(dto.getStatus());
			if (statusSelecionado == null) {
				ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
			}
			entity.setStatus(statusSelecionado);
			service.atualizar(entity);
			return ResponseEntity.ok(entity);
		}).orElseGet( () -> new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST) );
	}
	
	@GetMapping("{id}")
	public ResponseEntity obterLancamento ( @PathVariable("id") Long id ) {
		return service.obterPorId(id)
				.map( lancamento -> new ResponseEntity( converter(lancamento), HttpStatus.OK) )
				.orElseGet( () -> new ResponseEntity(HttpStatus.NOT_FOUND) );
	}
	
	@GetMapping
	public ResponseEntity buscar(
			//@RequestParam java.util.Map<String, String> params, aqui todos são opcionais
			//ou
			@RequestParam(value="descricao", required=false) String descricao,
			@RequestParam(value="tipo", required=false) String tipo,
 			@RequestParam(value="mes", required=false) Integer mes,
 			@RequestParam(value="ano",required=false) Integer ano,
 			@RequestParam("usuario") Long idUsuario
			) {
		Lancamento lancamentoFiltro = new Lancamento();
		lancamentoFiltro.setDescricao(descricao);
		lancamentoFiltro.setMes(mes);
		lancamentoFiltro.setAno(ano);
		lancamentoFiltro.setTipo( tipo == null || !tipo.isEmpty() ? null: TipoLancamento.valueOf(tipo) );
		
		Optional<Usuario> usuario = usuarioService.finById(idUsuario);
		if (!usuario.isPresent()) {
			return ResponseEntity.badRequest().body("Não foi possível realizar a consulta. Usuário não encontrado para o id informado.");
		} else {
			lancamentoFiltro.setUsuario(usuario.get());
		}
		
		List<Lancamento> lancamentos = service.buscar(lancamentoFiltro);
		
		return ResponseEntity.ok(lancamentos);
	}
	
	@PostMapping
	public ResponseEntity salvar( @RequestBody LancamentoDTO dto ) {
		try {
			Lancamento lancamento = converter(dto);
			lancamento = service.salvar(lancamento);
			return new ResponseEntity(lancamento, HttpStatus.CREATED);
		} catch (RegraNegocioException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@PutMapping("/atualizar/{id}")
	public ResponseEntity atualizar( @PathVariable Long id, @RequestBody LancamentoDTO dto) {
		return service.obterPorId(id).map( entity -> {
			try {
				Lancamento lancamento = converter(dto);
				lancamento.setId(entity.getId());
				service.atualizar(lancamento);
				return ResponseEntity.ok(lancamento);
			} catch (Exception e) {
				return ResponseEntity.badRequest().body(e.getMessage());
			}
		}).orElseGet( () -> new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST) );
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity deletar ( @PathVariable("id") Long id ) {
		return service.obterPorId(id).map( entity -> {
			try {
				service.deletar(entity);
				return new ResponseEntity( HttpStatus.NO_CONTENT );
			} catch (Exception e) {
				return ResponseEntity.badRequest().body(e.getMessage());
			}
		}).orElseGet( () -> new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST) );
	}
	
	private LancamentoDTO converter(Lancamento lancamento) {
		return LancamentoDTO.builder()
				.id(lancamento.getId())
				.descricao(lancamento.getDescricao())
				.valor(lancamento.getValor())
				.mes(lancamento.getMes())
				.ano(lancamento.getAno())
				.status(lancamento.getStatus().name())
				.tipo(lancamento.getTipo().name())
				.usuario(lancamento.getUsuario().getId())
				.build();
	}
	
	private Lancamento converter(LancamentoDTO dto) {
		
		Lancamento lancamento = new Lancamento();
		lancamento.setId(dto.getId());
		lancamento.setDescricao(dto.getDescricao());
		lancamento.setAno(dto.getAno());
		lancamento.setMes(dto.getMes());
		lancamento.setValor(dto.getValor());
		
		Usuario usuario = usuarioService.finById(dto.getUsuario())
				.orElseThrow( () -> new RegraNegocioException("Usuário não encontra para o id informado."));
		
		lancamento.setUsuario(usuario);
		if (dto.getTipo() != null) {
			lancamento.setTipo(TipoLancamento.valueOf(dto.getTipo()));
		}
		if (dto.getStatus() != null) {
			lancamento.setStatus(StatusLancamento.valueOf(dto.getStatus()));
		}
		
		return lancamento;
	}
}
