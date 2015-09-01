package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.extract.*;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.io.file.Files;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author Ilya Sadykov
 * Hacky strategy of extraction. Allows to extract the full postgres binaries.
 */
public class PostgresFilesToExtract extends FilesToExtract {
    final FileSet fileSet;
    final IDirectory extractDir;

    public PostgresFilesToExtract(IDirectory dirFactory, ITempNaming executableNaming, FileSet fileSet) {
        super(dirFactory, executableNaming, fileSet);
        this.fileSet = fileSet;
        this.extractDir = dirFactory;
    }

    /**
     * This is actually the very dirty hack method to adopt the Flapdoodle's API to the compatible way to extract and run
       TODO: very very hacky method!
     */
    @Override
    public IExtractionMatch find(final IArchiveEntry entry) {
        return new IExtractionMatch() {
            @Override
            public File write(InputStream source, long size) throws IOException {
                boolean isSymLink = false;
                String linkName = "";
                if (entry instanceof CommonsArchiveEntryAdapter) {
                    try {
                        // hack to allow symlinks extraction (ONLY tar archives are supported!)
                        Field archiveEntryField = CommonsArchiveEntryAdapter.class.getDeclaredField("_entry");
                        archiveEntryField.setAccessible(true);
                        ArchiveEntry archiveEntry = (ArchiveEntry) archiveEntryField.get(entry);
                        if (archiveEntry instanceof TarArchiveEntry) {
                            if (isSymLink = ((TarArchiveEntry) archiveEntry).isSymbolicLink()) {
                                linkName = ((TarArchiveEntry) archiveEntry).getLinkName();
                            }
                        }
                        archiveEntry.getSize();
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException("Check the version of de.flapdoodle.embed.process API. " +
                                "Has it changed?", e);
                    }
                }
                if (extractDir == null || extractDir.asFile() == null) {
                    return null;
                }

                // I got some problems with concurrency. Not sure this is required.
                synchronized (PostgresFilesToExtract.class) {
                    final String basePath = extractDir.asFile().getPath();
                    final File outputFile = Paths.get(basePath, entry.getName()).toFile();
                    if (entry.isDirectory()) {
                        if (!outputFile.exists()) {
                            Files.createDir(outputFile);
                        }
                    } else {
                        if (!outputFile.exists()) { // prevent double extraction (for other binaries)
                            if (isSymLink) {
                                try {
                                    final Path target = outputFile.toPath().getParent().resolve(Paths.get(linkName));
                                    java.nio.file.Files.createSymbolicLink(outputFile.toPath(), target);
                                } catch (Exception ignored) {
                                    // do nothing
                                }
                            } else {
                                Files.write(source, outputFile);
                            }
                        }
                        // hack to mark binaries as executable
                        if ((entry.getName().matches("pgsql/bin/.+"))) {
                            outputFile.setExecutable(true);
                        }
                    }
                    return outputFile;
                }
            }

            @Override
            public FileType type() {
                // does this archive entry match to any of the provided fileset entries?
                for (FileSet.Entry matchingEntry : fileSet.entries()) {
                    if (matchingEntry.matchingPattern().matcher(entry.getName()).matches()) {
                        return matchingEntry.type();
                    }
                }
                // Otherwise - it's just an library file
                return FileType.Library;
            }
        };
    }
}
