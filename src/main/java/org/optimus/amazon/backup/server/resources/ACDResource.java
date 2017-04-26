package org.optimus.amazon.backup.server.resources;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServletResponse;

import org.optimus.amazon.backup.server.services.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/acd")
public class ACDResource extends AbstractResource {

	private final static Logger LOGGER = LoggerFactory.getLogger(ACDResource.class);
	
	@Autowired
	private FileService fileService;
	
	@RequestMapping(method = RequestMethod.POST)
	public String pushToACD(@RequestParam(required = false, name = "path") String path) throws Exception {

		LOGGER.info("User {} push to ACD {}", getUser(), path);

		return fileService.pushToACD(path);
	}
}
