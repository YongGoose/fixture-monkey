/*
 * Fixture Monkey
 *
 * Copyright (c) 2021-present NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.fixturemonkey.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.navercorp.fixturemonkey.buildergroup.ArbitraryBuilderCandidate;

public final class ArbitraryCandidateManager {

	private final Map<String, ArbitraryBuilderCandidate<?>> candidateMap = new HashMap<>();

	private ArbitraryCandidateManager() {
	}

	public static ArbitraryCandidateManager create() {
		return new ArbitraryCandidateManager();
	}

	public ArbitraryCandidateManager add(String arbitraryBuilderName, ArbitraryBuilderCandidate<?> candidate) {
		checkNull(arbitraryBuilderName);
		checkDuplicate(arbitraryBuilderName);
		candidateMap.put(arbitraryBuilderName, candidate);

		return this;
	}

	private void checkNull(String arbitraryBuilderName) {
		if (arbitraryBuilderName == null) {
			throw new IllegalArgumentException("ArbitraryBuilder name must not be null");
		}
	}

	private void checkDuplicate(String arbitraryBuilderName) {
		if (candidateMap.containsKey(arbitraryBuilderName)) {
			throw new IllegalArgumentException("Duplicated ArbitraryBuilder name: " + arbitraryBuilderName);
		}
	}

	public Map<String, ArbitraryBuilderCandidate<?>> getCandidates() {
		return Collections.unmodifiableMap(candidateMap);
	}
}
