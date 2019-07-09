package fr.esgi.web.rest;

import static fr.esgi.config.Utils.getLang;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import fr.esgi.config.ErrorMessage;
import fr.esgi.exception.BurgerSTerminalException;
import fr.esgi.service.CommandService;
import fr.esgi.service.dto.CommandDTO;

/**
 * REST controller for managing the command
 */
@RestController
@RequestMapping("/api")
public class CommandResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommandResource.class);

	private final CommandService commandService;

	private final MessageSource messageSource;

	@Autowired
	public CommandResource(CommandService commandService, MessageSource messageSource) {
		this.commandService = commandService;
		this.messageSource = messageSource;
	}

	/**
	 * GET  /commands : get all commands by customerId.
	 * 
	 * @param commandDTO
	 * @return the ResponseEntity with status 200 (Ok) and with body the assignmentModuleDTO
	 * @throws URISyntaxException if the Location URI syntax is incorrect
	 * @throws BurgerSTerminalException if the id of command is not empty.
	 */
	@GetMapping("/commands")
	public ResponseEntity<Page<CommandDTO>> getAllCommands(
			@RequestParam("page") int page,
			@RequestParam("size") int size,
			@RequestParam("customerId") Long customerId) throws URISyntaxException, BurgerSTerminalException {
		LOGGER.debug("REST request to get all commands: {} {} {}", page, size, customerId);
		Page<CommandDTO> commandDTOs = commandService.findAllByCustomerId(PageRequest.of(page, size), customerId);
		if (commandDTOs.isEmpty()) {
			throw new BurgerSTerminalException(HttpStatus.NOT_FOUND.value(),
					messageSource.getMessage(ErrorMessage.ERROR_NOT_COMMANDS, null, getLang("fr")));
		}
		return ResponseEntity.ok()
				.body(commandDTOs);
	}

	/**
	 * POST  /commands : create a command.
	 * 
	 * @param commandDTO
	 * @return the ResponseEntity with status 201 (Created) and with body the assignmentModuleDTO
	 * @throws URISyntaxException if the Location URI syntax is incorrect
	 * @throws BurgerSTerminalException if the id of command is not empty.
	 */
	@PostMapping("/commands")
	public ResponseEntity<CommandDTO> createCommand(@RequestBody @Valid CommandDTO commandDTO) throws URISyntaxException, BurgerSTerminalException {
		LOGGER.debug("REST request to create a command: {}", commandDTO);
		if (null != commandDTO.getId()) {
			throw new BurgerSTerminalException(HttpStatus.BAD_REQUEST.value(),
					messageSource.getMessage(ErrorMessage.ERROR_NEW_COMMAND_ID_EXIST, null, getLang("fr")));
		}
		CommandDTO result = commandService.save(commandDTO);
		return ResponseEntity.created(new URI("/commands/" + result.getId()))
				.build();
	}

	/**
	 * PUT  /commands : update a command.
	 * 
	 * @param commandDTO
	 * @return the ResponseEntity with status 200 (OK) and with body the assignmentModuleDTO
	 * @throws BurgerSTerminalException if the id of command is empty.
	 */
	@PutMapping("/commands")
	public ResponseEntity<CommandDTO> updateCommand(@RequestBody @Valid CommandDTO commandDTO) throws BurgerSTerminalException {
		LOGGER.debug("REST request to update a command: {}", commandDTO);
		if (null == commandDTO.getId()) {
			throw new BurgerSTerminalException(HttpStatus.BAD_REQUEST.value(),
					messageSource.getMessage(ErrorMessage.ERROR_COMMAND_MUST_HAVE_ID, null, getLang("fr")));
		} 
		CommandDTO result = commandService.update(commandDTO);
		return ResponseEntity.ok()
				.body(result);
	}

	/**
	 * GET  /commands/{id}: get a command.
	 * 
	 * @param id the id of the commandDTO to retrieve
	 * @return the ResponseEntity with status 200 (OK) and with body the assignmentModuleDTO
	 * @throws BurgerSTerminalException if the command does not exists.
	 */
	@GetMapping("/commands/{id}")
	public ResponseEntity<CommandDTO> getCommand(@PathVariable Long id) throws BurgerSTerminalException {
		LOGGER.debug("REST request to get a command: {}", id);
		Optional<CommandDTO> commandDTO = commandService.findOne(id);
		if (commandDTO.isPresent()) {
			return ResponseEntity.ok().body(commandDTO.get());
		} else {
			throw new BurgerSTerminalException(HttpStatus.NOT_FOUND.value(), 
					messageSource.getMessage(ErrorMessage.ERROR_COMMAND_NOT_FOUND, null, getLang("fr")));
		}
	}

	/**
	 * DELETE  /commands/{id} : delete a command.
	 * 
	 * @param id the id of the commandDTO to delete
	 * @return the ResponseEntity with status 200 (OK)
	 */
	@DeleteMapping("/commands/{id}")
	public ResponseEntity<Void> deleteCommand(@PathVariable Long id) {
		LOGGER.debug("REST request to delete a command: {}", id);
		commandService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
