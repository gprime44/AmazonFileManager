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
@RequestMapping("/download")
public class DownloadResource extends AbstractResource {

	private final static Logger LOGGER = LoggerFactory.getLogger(DownloadResource.class);

	@Autowired
	private FileService fileService;

	@RequestMapping(method = RequestMethod.GET)
	public void download(@RequestParam(required = false, name = "path") String path, //
			HttpServletResponse response) throws Exception {

		LOGGER.info("User {} download {}", getUser(), path);

		Path file = fileService.getFileInGlobalFolder(path);

		response.addHeader("Content-Length", String.valueOf(Files.size(file)));
		response.addHeader("Content-Type", Files.probeContentType(file));
		response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getFileName().toString() + "\"");

		Files.copy(file, response.getOutputStream());
		response.flushBuffer();
	}
}
