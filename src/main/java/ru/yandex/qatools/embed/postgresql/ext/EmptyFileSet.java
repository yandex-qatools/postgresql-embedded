package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Ilya Sadykov
 */
public class EmptyFileSet implements IExtractedFileSet {
    @Override
    public File executable() {
        return null;
    }

    @Override
    public List<File> files(FileType type) {
        return Collections.emptyList();
    }

    @Override
    public File baseDir() {
        return null;
    }

    @Override
    public boolean baseDirIsGenerated() {
        return false;
    }
}
