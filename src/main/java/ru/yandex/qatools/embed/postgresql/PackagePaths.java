package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.IVersion;
import de.flapdoodle.embed.process.io.directories.IDirectory;

/**
 * Paths builder
 */
public class PackagePaths implements IPackageResolver {

    private final Command command;
    private final IDirectory tempDir;

    public PackagePaths(Command command, IDirectory tempDir) {
        this.command = command;
        this.tempDir = tempDir;
    }

    protected static String getVersionPart(IVersion version) {
        return version.asInDownloadPath();
    }

    public IDirectory getTempDir() {
        return tempDir;
    }

    @Override
    public FileSet getFileSet(Distribution distribution) {
        String cmdPattern;
        switch (distribution.getPlatform()) {
            case Linux:
            case OS_X:
                cmdPattern = command.commandName();
                break;
            case Windows:
                cmdPattern = command.commandName() + ".exe";
                break;
            default:
                throw new IllegalArgumentException("Unknown Platform "
                        + distribution.getPlatform());
        }
        try {
            return FileSet.builder()
                    .addEntry(FileType.Executable, tempDir.asFile().getPath(),
                            "^.*pgsql\\\\bin\\\\"+ cmdPattern+ "$")
                                    .addEntry(FileType.Executable, tempDir.asFile().getPath(),
                            "^.*pgsql/bin/" + cmdPattern + "$")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArchiveType getArchiveType(Distribution distribution) {
        ArchiveType archiveType;
        switch (distribution.getPlatform()) {
            case Linux:
                archiveType = ArchiveType.TGZ;
                break;
            case OS_X:
            case Windows:
                archiveType = ArchiveType.ZIP;
                break;
            default:
                throw new IllegalArgumentException("Unknown Platform "
                        + distribution.getPlatform());
        }
        return archiveType;
    }

    @Override
    public String getPath(Distribution distribution) {
        String sversion = getVersionPart(distribution.getVersion());

        ArchiveType archiveType = getArchiveType(distribution);
        String sarchiveType;
        switch (archiveType) {
            case TGZ:
                sarchiveType = "tar.gz";
                break;
            case ZIP:
                sarchiveType = "zip";
                break;
            default:
                throw new IllegalArgumentException("Unknown ArchiveType "
                        + archiveType);
        }

        String splatform;
        switch (distribution.getPlatform()) {
            case Linux:
                splatform = "linux";
                break;
            case Windows:
                splatform = "windows";
                break;
            case OS_X:
                splatform = "osx";
                break;
            default:
                throw new IllegalArgumentException("Unknown Platform "
                        + distribution.getPlatform());
        }

        String bitsize = "";
        switch (distribution.getBitsize()) {
            case B32:
                switch (distribution.getPlatform()) {
                    case Windows:
                    case Linux:
                    case OS_X:
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "32 bit supported only on Windows, MacOS, Linux, platform is "
                                        + distribution.getPlatform());
                }
                break;
            case B64:
                switch (distribution.getPlatform()) {
                    case Linux:
                    case Windows:
                        bitsize = "-x64";
                        break;
                    case OS_X:
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "64 bit supported only on Linux and Windows, platform is "
                                        + distribution.getPlatform());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown BitSize " + distribution.getBitsize());
        }

        return "postgresql-" + sversion + "-" + splatform + bitsize + "-binaries" + "." + sarchiveType;
    }
}