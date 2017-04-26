package org.optimus.amazon.backup.server.resources;

import org.apache.commons.lang3.StringUtils;
import org.optimus.amazon.backup.server.dto.FolderDto;
import org.optimus.amazon.backup.server.dto.UserDto;
import org.optimus.amazon.backup.server.services.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/content")
public class ContentResource extends AbstractResource {

	private final static Logger LOGGER = LoggerFactory.getLogger(ContentResource.class);

	@Autowired
	private FileService fileService;

	@RequestMapping(method = RequestMethod.GET)
	public FolderDto getFolderContent(@RequestParam(required = false, name = "path") String path, //
			@RequestParam("withFile") boolean withFile) throws Exception {

		LOGGER.info("User {} get folder content {}", getUser(), path);

		if (StringUtils.isEmpty(path)) {
			path = StringUtils.EMPTY;
		}

		return fileService.getFolderContent(path, withFile);
	}

}