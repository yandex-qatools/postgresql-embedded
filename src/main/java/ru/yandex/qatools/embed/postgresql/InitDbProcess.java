/**
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	konstantin-ba@github, Archimedes Trajano (trajano@github), Christian Bayer (chrbayer84@googlemail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.Platform;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.LogWatchStreamProcessor;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.StreamToLineProcessor;
import de.flapdoodle.embed.process.io.file.Files;
import de.flapdoodle.embed.process.runtime.ProcessControl;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.ext.SubdirTempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static de.flapdoodle.embed.process.io.file.Files.createTempFile;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

/**
 * initdb process
 * (helper to initialize the DB)
 */
class InitDbProcess<E extends InitDbExecutable> extends AbstractPGProcess<E, InitDbProcess> {

    public InitDbProcess(Distribution distribution, PostgresConfig config, IRuntimeConfig runtimeConfig, E executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, PostgresConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        ret.add(exe.executable().getAbsolutePath());
        if (getConfig().credentials() != null) {
            final File pwFile = createTempFile(SubdirTempDir.defaultInstance(), "pwfile" + randomUUID());
            pwFile.deleteOnExit();
            Files.write(getConfig().credentials().password(), pwFile);
            ret.addAll(asList(
                    "-A", "password",
                    "-U", getConfig().credentials().username(),
                    "--pwfile=" + pwFile.getAbsolutePath()
            ));
        }
        if (distribution.getPlatform() == Platform.Windows) {
            ret.addAll(config.getAdditionalInitDbParams());
        }
        ret.add(config.storage().dbDir().getAbsolutePath());
        if (distribution.getPlatform() != Platform.Windows) {
            ret.addAll(config.getAdditionalInitDbParams());
        }
        return ret;
    }

    @Override
    protected void onAfterProcessStart(ProcessControl process, IRuntimeConfig runtimeConfig) throws IOException {
        ProcessOutput outputConfig = runtimeConfig.getProcessOutput();
        LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(
                "database system is ready to accept connections",
                new HashSet<>(asList("[initdb error]")), StreamToLineProcessor.wrap(outputConfig.getOutput()));
        Processors.connect(process.getReader(), logWatch);
        Processors.connect(process.getError(), StreamToLineProcessor.wrap(outputConfig.getError()));
        logWatch.waitForResult(getConfig().timeout().startupTimeout());
    }
}