package fr.esgi.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.esgi.config.Constants;
import fr.esgi.dao.MenuRepository;
import fr.esgi.domain.Menu;
import fr.esgi.domain.Product;
import fr.esgi.service.MenuService;
import fr.esgi.service.dto.MenuDTO;
import fr.esgi.service.dto.ProductDTO;
import fr.esgi.service.mapper.MenuMapper;
import fr.esgi.service.mapper.ProductMapper;

/**
 * Service Implementation for managing Menu.
 */
@Transactional
@Service("MenuService")
public class MenuServiceImpl implements MenuService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MenuServiceImpl.class);

	private final MenuRepository menuRepository;

	private final MenuMapper menuMapper;

	private final ProductMapper productMapper;

	@Autowired
	public MenuServiceImpl(MenuRepository menuRepository, MenuMapper menuMapper, ProductMapper productMapper) {
		this.menuRepository = menuRepository;
		this.menuMapper = menuMapper;
		this.productMapper = productMapper;
	}

	/**
	 * Get all the menus.
	 * 
	 * @return the list of entities 
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<MenuDTO> findAll(int page,int size) {
		LOGGER.debug("Request to get all products");
		return menuRepository.findAll(PageRequest.of(page, size))
				.map(menuMapper::menuToMenuDTO);
	}

	/**
	 * Get all the products by menu id.
	 * 	
	 * @return the list of entities
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ProductDTO> findProductsByMenuId(Long id, String categoryName) {
		Optional<List<Product>> map = menuRepository.findById(id)
				.map(Menu::getProducts);
		if (map.isPresent()) {
			return map.get().stream()
					.filter(p -> categoryName.equalsIgnoreCase(p.getCategory().getName()))
					.map(productMapper::productToProductDTO)
					.distinct()
					.collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the four trends menus.
	 * 
	 * @return the list of entities
	 */
	@Transactional(readOnly = true)
	@Override
	public List<MenuDTO> findAllTrendsMenus() {
		List<MenuDTO> menusDTO = menuRepository.findAllTrendsMenus(Constants.PAID, 4).stream()
				.map(menuMapper::menuToMenuDTO)
				.collect(Collectors.toList());
		Collections.shuffle(menusDTO);
		return menusDTO;
	}

	/**
	 * Save all the menus.
	 * 
	 * @param menusDTO the list of entities to save
	 * @return the list of entities
	 */	
	@Override
	public List<MenuDTO> saveAll(List<MenuDTO> menusDTO) {
		LOGGER.debug("Request to save all menus");
		List<Menu> menus = menusDTO.stream()
				.map(menuMapper::menuDTOToMenu)
				.collect(Collectors.toList());

		return menuRepository.saveAll(menus).stream()
				.map(menuMapper::menuToMenuDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Delete the "id" menu.
	 *
	 * @param id the id of the entity
	 */
	@Override
	public void delete(Long id) {
		LOGGER.debug("Request to delete a menu");
		menuRepository.deleteById(id);
	}

	/**
	 * Update a menu.
	 *
	 * @param menuDTO the entity to update
	 * @return the persisted entity
	 */
	@Override
	public MenuDTO update(MenuDTO menuDTO) {
		LOGGER.debug("Request to update a menu: {}", menuDTO);
		Menu menu = menuMapper.menuDTOToMenu(menuDTO);
		menu = menuRepository.saveAndFlush(menu);
		return menuMapper.menuToMenuDTO(menu); 
	}

	/**
	 * Save a menu.
	 *
	 * @param menuDTO the entity to save
	 * @return the persisted entity
	 */
	@Override
	public MenuDTO save(MenuDTO menuDTO) {
		LOGGER.debug("Request to save a menu: {}", menuDTO);
		Menu menu = menuMapper.menuDTOToMenu(menuDTO);	
		menu = menuRepository.save(menu);
		return menuMapper.menuToMenuDTO(menu);
	}
}
