/**
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	konstantin-ba@github,Archimedes Trajano (trajano@github)
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

import de.flapdoodle.embed.process.distribution.BitSize;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.IVersion;
import de.flapdoodle.embed.process.distribution.Platform;
import de.flapdoodle.embed.process.store.IArtifactStore;
import junit.framework.TestCase;
import org.junit.Assert;
import ru.yandex.qatools.embed.postgresql.ext.ArtifactStoreBuilder;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.io.IOException;

public class TestDownloads extends TestCase {

	public void testDownloads() throws IOException {
		IArtifactStore artifactStore = new ArtifactStoreBuilder().defaults(Command.Postgres).build();

		for (Platform p : Platform.values()) {
			for (BitSize b : BitSize.values()) {
				for (IVersion version : Version.Main.values()) {
					Distribution distribution = new Distribution(version, p, b);
					Assert.assertTrue("Distribution: " + distribution, artifactStore.checkDistribution(distribution));
				}
			}
		}
	}
}
