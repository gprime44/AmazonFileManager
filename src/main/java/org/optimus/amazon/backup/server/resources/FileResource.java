package org.optimus.amazon.backup.server.resources;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.optimus.amazon.backup.server.dto.FolderDto;
import org.optimus.amazon.backup.server.services.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/content")
public class FileResource extends AbstractResource {

	private final static Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

	@Autowired
	private FileService fileService;

	@Value("${admin.login}")
	private String ADMIN_LOGIN;

	@RequestMapping(method = RequestMethod.GET)
	public FolderDto getFolderContent(@RequestParam(required = false, name = "path") String path, //
			@RequestParam("withFile") boolean withFile) throws Exception {

		LOGGER.info("User {} get folder content {}", getUser(), path);

		if (StringUtils.isEmpty(path)) {
			path = StringUtils.EMPTY;
		}

		return fileService.getFolderContent(path, withFile);
	}

	@RequestMapping(value = "/download", method = RequestMethod.GET)
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

	@RequestMapping(value = "/get", method = RequestMethod.GET)
	public void get(@RequestParam(required = false, name = "path") String path, //
			HttpServletResponse response) throws Exception {

		LOGGER.info("User {} get {}", getUser(), path);

		Path file = fileService.getFileInGlobalFolder(path);

		response.addHeader("Content-Length", String.valueOf(Files.size(file)));
		response.addHeader("Content-Type", Files.probeContentType(file));
		response.addHeader("Content-Disposition", "inline; filename=\"" + file.getFileName().toString() + "\"");

		Files.copy(file, response.getOutputStream());
		response.flushBuffer();
	}

	@RequestMapping(method = RequestMethod.DELETE)
	public void delete(@RequestParam("path") String path) throws Exception {

		String user = getUser();

		if (StringUtils.equals(user, ADMIN_LOGIN)) {
			LOGGER.info("User {} delete {}", path);
			fileService.delete(path);
		} else {
			LOGGER.info("User {} can't delete file", user);
		}
	}

	@RequestMapping(method = RequestMethod.POST)
	public void addFile(@RequestParam("path") String path, //
			@RequestParam("file") MultipartFile[] files) throws Exception {

		String user = getUser();

		if (StringUtils.equals(user, ADMIN_LOGIN)) {
			LOGGER.info("User {} add files to {}", user, path);
			fileService.saveFiles(path, files);
		} else {
			LOGGER.info("User {} can't add file", user);
		}
	}

}