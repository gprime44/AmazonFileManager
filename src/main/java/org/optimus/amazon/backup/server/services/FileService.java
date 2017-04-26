package org.optimus.amazon.backup.server.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.optimus.amazon.backup.server.dto.FileDto;
import org.optimus.amazon.backup.server.dto.FileDto.STATE;
import org.optimus.amazon.backup.server.dto.FolderDto;
import org.optimus.amazon.backup.server.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService extends AbstractService {

	private final static Logger LOGGER = LoggerFactory.getLogger(FileService.class);

	public FolderDto getFolderContent(String folder, boolean withFile) throws ServiceException {
		Path globalRootPath = Paths.get(localRootFolder).resolve(localGlobalFolder);
		Path localDecodedPath = Paths.get(localRootFolder).resolve(localDecodedFolder);
		Path remoteRootPath = Paths.get(localRootFolder).resolve(remoteDecodedFolder);

		Path folderToScan = getFileInGlobalFolder(folder);

		if (!Files.exists(folderToScan)) {
			throw new ServiceException("Folder {} doesn't exist", folderToScan);
		}

		if (!Files.isDirectory(folderToScan)) {
			throw new ServiceException("{} isn't a folder", folderToScan);
		}

		FolderDto folderDto = new FolderDto();
		folderDto.setPath(folder);
		folderDto.setName(folderToScan.getFileName().toString());
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderToScan)) {
			for (Path entry : stream) {
				boolean isLocal = Files.exists(localDecodedPath.resolve(globalRootPath.relativize(entry)));
				boolean isRemote = Files.exists(remoteRootPath.resolve(globalRootPath.relativize(entry)));
				STATE state = STATE.REMOTE;
				if (isLocal) {
					state = STATE.LOCAL;
					if (isRemote) {
						state = STATE.BOTH;
					}
				}

				if (Files.isDirectory(entry)) {
					FolderDto subFolder = new FolderDto();
					subFolder.setName(entry.getFileName().toString());
					subFolder.setSize(Files.size(entry));
					subFolder.setState(state);
					subFolder.setPath(globalRootPath.relativize(entry).toString());
					subFolder.setDateUpdate(new Date(Files.getLastModifiedTime(entry).toMillis()));
					folderDto.getFolders().add(subFolder);
					folderDto.setSize(folderDto.getSize() + subFolder.getSize());

				} else if (withFile) {
					FileDto fileDto = new FileDto();
					fileDto.setName(entry.getFileName().toString());
					fileDto.setSize(Files.size(entry));
					fileDto.setState(state);
					fileDto.setPath(globalRootPath.relativize(entry).toString());
					fileDto.setDateUpdate(new Date(Files.getLastModifiedTime(entry).toMillis()));
					folderDto.getFiles().add(fileDto);
					folderDto.setSize(folderDto.getSize() + fileDto.getSize());
				}
			}

			LOGGER.debug("Found {} folders and {} files", folderDto.getFolders().size(), folderDto.getFiles().size());

		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}

		Collections.sort(folderDto.getFolders(), new Comparator<FolderDto>() {

			@Override
			public int compare(FolderDto o1, FolderDto o2) {
				return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			}
		});

		Collections.sort(folderDto.getFiles(), new Comparator<FileDto>() {

			@Override
			public int compare(FileDto o1, FileDto o2) {
				return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			}
		});

		return folderDto;
	}

	public Path getFileInGlobalFolder(String path) throws ServiceException {
		Path globalRootFolder = Paths.get(localRootFolder).resolve(localGlobalFolder);

		Path file = globalRootFolder.resolve(path);
		if (!Files.exists(file)) {
			throw new ServiceException("File {} does't exist", file);
		}
		if (!Files.isReadable(file)) {
			throw new ServiceException("File {} isn't readable", file);
		}

		return file;
	}

	public void delete(String path) throws ServiceException {
		Path localFile = Paths.get(localRootFolder).resolve(localDecodedFolder).resolve(path);
		Path remotePath = Paths.get(localRootFolder).resolve(remoteDecodedFolder);
		Path remoteFile = remotePath.resolve(path);

		if (Files.exists(localFile)) {
			LOGGER.debug("File {} exist in local storage {}", path, localFile);
			try {
				Files.delete(localFile);
			} catch (IOException e) {
				throw new ServiceException("Unable to delete {}", localFile);
			}
		}

		if (Files.exists(remoteFile)) {
			LOGGER.debug("File {} exist in remote storage {}", path, remoteFile);

			String encodedPath = getEncodedPath(remotePath, path);

			Path remoteEncodedPath = Paths.get(localRootFolder).resolve(remoteEncodedFolder).resolve(encodedPath);
			if (Files.exists(remoteEncodedPath)) {
				LOGGER.debug("Encoded path {} found, so delete it on ACD", remoteEncodedPath);
				CommandLine cl = new CommandLine("acd_cli");
				cl.addArgument("trash");
				cl.addArgument(encodedPath);

				LOGGER.debug("Execute : {}", cl.toString());

				try {
					LOGGER.info("Execution return : {}", new DefaultExecutor().execute(cl));
				} catch (ExecuteException e) {
					LOGGER.error(e.getMessage(), e);
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
	}

	private String getEncodedPath(Path rootPath, String path) throws ServiceException {
		LOGGER.debug("Get encoded path for {} on {}", path, rootPath);

		CommandLine cl = new CommandLine("encfsctl");
		cl.addArgument("encode");
		cl.addArgument(rootPath.toAbsolutePath().toString());
		cl.addArgument(path);

		LOGGER.debug("Execute : {}", cl.toString());

		Map<String, String> env = new HashMap<>();
		env.put("ENCFS6_CONFIG", encfsFile);
		try (OutputStream os = new ByteArrayOutputStream(); InputStream is = new ByteArrayInputStream(encfsPassword.getBytes());) {
			DefaultExecutor executor = new DefaultExecutor();
			ExecuteStreamHandler streamHandler = new PumpStreamHandler(os, os, is);
			executor.setStreamHandler(streamHandler);
			LOGGER.debug("Execution return code : {}", executor.execute(cl, env));

			String encodedPath = os.toString();
			LOGGER.debug("Encoded path for {} is {}", path, encodedPath);
			return encodedPath;
		} catch (ExecuteException e) {
			throw new ServiceException("Unable to retrieve encoded path", e);
		} catch (IOException e) {
			throw new ServiceException("Unable to retrieve encoded path", e);
		}
	}

	public void saveFiles(String path, MultipartFile[] files) throws ServiceException {
		if (ArrayUtils.isNotEmpty(files)) {
			for (MultipartFile file : files) {
				Path destPath = null;
				try {
					destPath = getFileInGlobalFolder(path).resolve(file.getOriginalFilename());
				} catch (ServiceException e) {
					throw new ServiceException("Unable to retrieve path {}", path);
				}
				try {
					LOGGER.debug("Save file to local path {}", destPath);
					Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new ServiceException("Unable to upload file {}", destPath);
				}
			}
		}
	}

	public String pushToACD(String fileToSave) throws ServiceException {
		Path localDecodedRootFolder = Paths.get(localRootFolder).resolve(localDecodedFolder);
		Path localEncodedRootFolder = Paths.get(localRootFolder).resolve(localEncodedFolder);

		Path localDecodedFile = localDecodedRootFolder.resolve(fileToSave);

		if (!Files.exists(localDecodedFile)) {
			throw new ServiceException("File {} doesn't exist in local folder {}", fileToSave, localDecodedRootFolder);
		}

		if (Files.isDirectory(localDecodedFile)) {

			return "BATCH_MODE";

		} else {
			String encodedFilePath = getEncodedPath(localDecodedRootFolder, fileToSave);

			Path fileToUpload = localEncodedRootFolder.resolve(encodedFilePath);

			if (!Files.exists(fileToUpload)) {
				throw new ServiceException("Encoded file {} doesn't exist", fileToUpload);
			}

			Path encodedPath = Paths.get(remoteRootFolder).resolve(encodedFilePath).getParent();

			LOGGER.debug("Upload {} to {}", fileToUpload.toAbsolutePath().toString(), encodedPath);

			CommandLine cmdLine = new CommandLine("acd_cli");
			cmdLine.addArgument("upload");
			cmdLine.addArgument(fileToUpload.toAbsolutePath().toString());
			cmdLine.addArgument(encodedPath.toString());

			ByteArrayOutputStream outputStream = null;
			try {
				DefaultExecutor executor = new DefaultExecutor();
				outputStream = new ByteArrayOutputStream();
				PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
				executor.setStreamHandler(streamHandler);
				long start = Calendar.getInstance().getTimeInMillis();
				executor.execute(cmdLine);
				LOGGER.debug("Upload done in {} sec", (Calendar.getInstance().getTimeInMillis() - start) / 1000);
				LOGGER.debug("Delete file {}", fileToUpload);
				Files.delete(fileToUpload);
				return "OK";
			} catch (Exception e) {
				throw new ServiceException("Unable to upload file {} ", e, fileToUpload);
			} finally {
				IOUtils.closeQuietly(outputStream);
			}
		}
	}

	// public void pushFolderToACD(Path folder) {
	// final Path localEncodedPath = Paths.get(args[0]);
	// final Path acdEncodedPath = Paths.get(args[1]);
	//
	// if (!Files.exists(localEncodedPath)) {
	// System.out.println("Input path doesn't exist");
	// System.exit(0);
	// }
	//
	// System.out.println("Upload " + localEncodedPath + " to " + acdEncodedPath);
	//
	// localPathOffset = localEncodedPath.getNameCount();
	//
	// try {
	// sync();
	//
	// Files.walkFileTree(localEncodedPath, new SimpleFileVisitor<Path>() {
	//
	// @Override
	// public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
	// if (!dir.equals(localEncodedPath) && !StringUtils.startsWith(dir.getFileName().toString(), ".")) {
	// log(dir, "Enter " + dir.getFileName().toString());
	//
	// Path toCreate = acdEncodedPath.resolve(localEncodedPath.relativize(dir));
	//
	// CommandLine cmdLine = new CommandLine("acd_cli");
	// cmdLine.addArgument("mkdir");
	// cmdLine.addArgument(toCreate.toAbsolutePath().toString());
	//
	// ByteArrayOutputStream outputStream = null;
	// try {
	// log(dir, "Create " + toCreate.toAbsolutePath().toString());
	// DefaultExecutor executor = new DefaultExecutor();
	// outputStream = new ByteArrayOutputStream();
	// PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
	// executor.setStreamHandler(streamHandler);
	// executor.execute(cmdLine);
	// log(dir, outputStream.toString());
	// } catch (Exception e) {
	// log(dir, e.getMessage());
	// } finally {
	// IOUtils.closeQuietly(outputStream);
	// }
	// }
	// return FileVisitResult.CONTINUE;
	// }
	//
	// @Override
	// public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
	// log(file, file.getFileName().toString());
	//
	// Path toUpload = acdEncodedPath.resolve(localEncodedPath.relativize(file)).getParent();
	//
	// CommandLine cmdLine = new CommandLine("acd_cli");
	// cmdLine.addArgument("upload");
	// cmdLine.addArgument(file.toAbsolutePath().toString());
	// cmdLine.addArgument(toUpload.toAbsolutePath().toString());
	//
	// ByteArrayOutputStream outputStream = null;
	// try {
	// log(file, "Upload to " + toUpload.toAbsolutePath().toString());
	// DefaultExecutor executor = new DefaultExecutor();
	// outputStream = new ByteArrayOutputStream();
	// PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
	// executor.setStreamHandler(streamHandler);
	// long start = Calendar.getInstance().getTimeInMillis();
	// executor.execute(cmdLine);
	// log(file, outputStream.toString() + "(" + (Calendar.getInstance().getTimeInMillis() - start) / 1000 + " sec)");
	// log(file, "Delete " + file.getFileName().toString());
	// Files.delete(file);
	// } catch (Exception e) {
	// log(file, e.getMessage());
	// } finally {
	// IOUtils.closeQuietly(outputStream);
	// }
	//
	// return FileVisitResult.CONTINUE;
	// }
	//
	// @Override
	// public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
	// if (!dir.equals(localEncodedPath)) {
	// log(dir, "Exit " + dir.getFileName().toString());
	// try {
	// log(dir, "Delete " + dir.getFileName().toString());
	// Files.delete(dir);
	// } catch (Exception e) {
	// log(dir, e.getMessage());
	// }
	// }
	// return FileVisitResult.CONTINUE;
	// }
	//
	// @Override
	// public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
	// log(file, e.getMessage());
	// return FileVisitResult.CONTINUE;
	// }
	//
	// });
	//
	// sync();
	//
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
}
